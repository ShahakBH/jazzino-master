#!/usr/bin/env ruby

require 'rubygems'
require 'bundler/setup'

require 'yaml'
require 'aws-sdk'
require 'rainbow'

require 'config'

class Diff

  def initialize(override_config, infer_names = true)
    @config = Yazino::Config.new(override_config)

    @infer_names = infer_names
  end

  def releases(from_release, to_release)
    throw "from_release may not be null" if from_release.nil?
    throw "to_release may not be null" if to_release.nil?

    work_dir = tmpdir()
    release_name_one = process_release_name(from_release)
    release_name_two = process_release_name(to_release)

    raise "You can't compare a release with itself." if release_name_one == release_name_two

    release_file_one = as_yaml(get_release_file(release_name_one, work_dir))
    release_file_two = as_yaml(get_release_file(release_name_two, work_dir))

    intersection = release_file_one['versions'].keys & release_file_two['versions'].keys
    removed_components = release_file_one['versions'].keys - intersection
    new_components = release_file_two['versions'].keys - intersection

    { :from_name => release_name_one, \
      :from_release => release_file_one, \
      :to_name => release_name_two, \
      :to_release => release_file_two, \
      :new => new_components, \
      :shared => intersection, \
      :removed => removed_components }
  end

  def print_diff(release_one, release_two)
    diff = releases(release_one, release_two)

    puts "Comparing #{diff[:from_name]} with #{diff[:to_name]}"
    puts "------------------------------------------------------------------\n"
    diff[:shared].sort.each do |component|
      if diff[:from_release]['versions'][component] != diff[:to_release]['versions'][component]
        print "-" + " M ".foreground(:blue) + "#{component} @ "
        print "#{diff[:from_release]['versions'][component]}".foreground(:red)
        print " -> "
        print "#{diff[:to_release]['versions'][component]}".foreground(:green)
      else
        print "-   #{component} @ #{diff[:from_release]['versions'][component]}"
      end
      print "\n"
    end

    diff[:removed].sort.each do |component|
      puts "-" + " D #{component} @ #{diff[:from_release]['versions'][component]}".foreground(:red)
    end

    diff[:new].sort.each do |component|
      puts "-" + " A #{component} @ #{diff[:to_release]['versions'][component]}".foreground(:green)
    end
  end

  private

  def as_yaml(filename)
    YAML.load_file(filename)
  end

  def process_release_name(release_name)
    release_name = release_name.strip
    if @infer_names && release_name !~ /^strata-release-/ && release_name !~ /^release-/
      "strata-release-#{release_name}"
    elsif @infer_names && release_name !~ /^strata-/
      "strata-#{release_name}"
    else
      release_name
    end
  end

  def get_release_file(release_name, destination)
    dest_file = "#{destination}/#{release_name}.release"

    s3 = AWS::S3.new(:access_key_id => @config.s3['access-key'], :secret_access_key => @config.s3['secret-key'])
    release_bucket = s3.buckets['yazino-releases']
    available_files = release_bucket.objects.collect {|obj| obj.key}

    raise "Error: Cannot find definition for artefact #{release_name}" if !available_files.include?("#{release_name}.release")
    File.open(dest_file, 'wb') do |file|
      release_bucket.objects["#{release_name}.release"].read do |chunk|
        file.write(chunk)
      end
    end
    raise "Error: Cannot download definition for artefact #{release_name}" unless File.exists?(dest_file)

    dest_file
  end

  def tmpdir()
    if !ENV['RELEASE_TMPDIR'].nil?
      filter_dir ENV['RELEASE_TMPDIR']
    elsif !ENV['TMPDIR'].nil?
      filter_dir ENV['TMPDIR']
    else
      "/tmp"
    end
  end

  def filter_dir(dirname)
    dirname =~ /\/$/ ? dirname[0..dirname.length - 2] : dirname
  end

end

if __FILE__ == $0
  def parse_arguments(args)
    raise "Usage: #{__FILE__} <release1> <release2> [--config=file.yaml] [--infer-names]" if args.length < 2

    release_one = args[0]
    release_two = args[1]

    override_config = nil
    infer_names = true

    (2..args.length - 1).each do |i|
      override_config = $1 if args[i] =~ /--config=(.*)/
    end

    [release_one, release_two, override_config, infer_names]
  end

  # begin
    release_one, release_two, override_config, infer_names = parse_arguments(ARGV)

    Diff.new(override_config, infer_names).print_diff(release_one, release_two)

  # rescue RuntimeError => e
  #   puts "Error: #{e}"
  #   exit 1
  # end

end
