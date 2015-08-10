require "rubygems"
require "bundler"

Bundler.setup(:test)

require "test/unit"
require "mocha"
require "deployment_checker"

module Mocha
  
  module ParameterMatchers
    def files(value)
      FilesMatcher.new(value)
    end
  end

  class FilesMatcher < Mocha::ParameterMatchers::Base
    def initialize(value)
      @value = value.uniq.sort
    end

    def matches?(available_parameters)
      parameter = available_parameters.shift
      param_files = []
      parameter.map do |element|
        param_files << element.scan(/[^\/]+/).last
      end
      param_files.uniq.sort == @value
    end

    def mocha_inspect
      "files(#{@value.mocha_inspect})"
    end
  end

end
