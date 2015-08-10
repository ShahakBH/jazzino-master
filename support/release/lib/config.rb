require 'yaml'

module Yazino
  class Config

    SMTP_SERVER = 'smtp.server'
    SMTP_PORT = 'smtp.port'
    SMTP_USER = 'smtp.user'
    SMTP_PASSWORD = 'smtp.password'
    DB_HOSTNAME = 'db.hostname'
    DB_NAME = 'db.name'
    DB_USER = 'db.user'
    DB_PASSWORD = 'db.password'
    NOTIFICATION_FROM = 'notification.from'
    NOTIFICATION_TO = 'notification.to'
    RELEASE_SERVER = 'release.server'
    RELEASE_USER = 'release.user'
    GIT_REPO = 'git.repo'
    RELEASE_PASSWORD = 'release.password'
    RELEASE_LOCATION = 'release.location'
    RELEASE_COMPONENTS = 'release.components.file'
    MAVEN_BASE = 'maven.base-version'
    MAVEN_OPTS = 'maven.opts'
    MAVEN_REPO = 'maven.repo'
    MAVEN_CMDS = 'maven.cmds'
    JIRA_HOST = 'jira.host'

    def initialize(override_config)
      @config = File.open(config_file) { |yf| YAML::load(yf) }

      if (override_config)
        raise "Cannot find override file: #{override_config}" if !File.exists?(override_config)

        override_hash = File.open(override_config) { |yf| YAML::load(yf) }
        @config = @config.merge(override_hash)
      end
    end

    def [](key)
      @config[key]
    end

    def debug?
      @config['release.debug']
    end

    def base_dir
      File.expand_path("#{File.dirname(__FILE__)}/..")
    end

    def s3
      @config['s3'] || []
    end

    private

    def config_file
      File.expand_path("#{base_dir}/config/release.yaml")
    end

  end
end
