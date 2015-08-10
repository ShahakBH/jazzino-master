require 'fileutils'

class Git

  def initialize(repository = nil, location = nil)
    @repository = repository
    @location = location

    verify_netrc_exists if (@repository =~ /^http/) == 0
  end

  def exists?
     File.directory? "#{@location}/.git"
  end

  def remove_working_copy
    FileUtils.rm_rf("#{@location}")
  end

  def clone
    return if File.directory? "#{@location}/.git"

    Dir.chdir("/tmp")
    clone_output = `git clone #{@repository} #{@location}`
    raise "Couldn't clone repository #{@repository}, output was #{clone_output}" if $?.exitstatus != 0
    Dir.chdir(@location)
  end

  def repository
    check_for_repository

    @location
  end

  def branch(branch_name)
    check_for_repository

    Dir.chdir(@location)
    branch_output = `git checkout -b #{branch_name}`
    raise "Branch failed, output was #{branch_output}" if $?.exitstatus != 0
  end

  def checkout(branch_name)
    check_for_repository

    Dir.chdir(@location)
    checkout_output = `git checkout #{branch_name}`
    raise "Checkout failed, output was #{checkout_output}" if $?.exitstatus != 0
  end

  def reset_hard?
    check_for_repository

    Dir.chdir(@location)
    reset_output = `git reset --hard`

    $?.exitstatus == 0
  end

  def pull(branch_name)
    check_for_repository

    Dir.chdir(@location)
    pull_output = `git pull origin #{branch_name}`
    raise "Pull failed, output was #{pull_output}" if $?.exitstatus != 0
  end

  def info
    check_for_repository

    Dir.chdir(@location)
    last_commit = `git --no-pager log --max-count=1 | grep -e '^commit' | awk '{ print $2 }'`
    remote_url = `git remote -v | grep fetch | awk '{ print $2 }'`

    [remote_url.strip, last_commit.strip]
  end

  def push(branch_name)
    check_for_repository

    Dir.chdir(@location)
    push_output = `git push origin #{branch_name}`
    raise "Push failed, output was #{push_output}" if $?.exitstatus != 0
  end

  def add(path_to_add)
    check_for_repository

    Dir.chdir(@location)

    add_output = `git add #{path_to_add}`
    raise "Add failed, output was #{add_output}" if $?.exitstatus != 0
    add_output
  end

  def commit(message, files = nil)
    check_for_repository

    Dir.chdir(@location)
    if files.nil?
      commit_output = `git commit -a -m'#{message}'`
    else
      commit_output = `git commit -m'#{message}' #{files}`
    end
    raise "Commit failed, output was #{commit_output}" if $?.exitstatus != 0
  end

  def log(first_revision = nil, last_revision = nil)
    check_for_repository

    Dir.chdir(@location)

    `git log #{first_revision}..#{last_revision} --pretty='format:%h %s [%an]'`
  end

  private

  def verify_netrc_exists
    if !File.exists?("#{ENV['HOME']}/.netrc") && %x(grep osxkeychain ~/.gitconfig) !~ /osxkeychain/
      puts "No .netrc file exists (and osxkeychain is not in use), please create one before running this script"
      exit 1
    end
  end

  def check_for_repository
    raise "Cannot find repository in #{@location}" if !File.directory? "#{@location}/.git"
  end

end
