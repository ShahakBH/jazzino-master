#!/usr/bin/env ruby

require 'net/smtp'
require 'socket'
require 'diff'
require 'rest_client'
require 'open-uri'

class Notifier

  def initialize(override_config)
    @config = Yazino::Config.new(override_config)
    @override_config = override_config

    smtp_username = @config[Yazino::Config::SMTP_USER]
    smtp_password = @config[Yazino::Config::SMTP_PASSWORD]

    @smtp_server = @config[Yazino::Config::SMTP_SERVER]
    @smtp_port = @config[Yazino::Config::SMTP_PORT]
    @smtp_username = smtp_username if smtp_username && smtp_username.strip.length > 0
    @smtp_password = smtp_password if smtp_password && smtp_password.strip.length > 0
    @smtp_auth = :plain if @smtp_username || @smtp_password

    @hipchat_server = @config['hipchat']['server']
    @hipchat_from = @config['hipchat']['from']
    @hipchat_room = @config['hipchat']['room']
    @hipchat_token = @config['hipchat']['token']
  end

  def send(recipient, sender, release_info)
    send_email(recipient, sender, release_info)
    send_hipchat(recipient, sender, release_info)
  end

  private

  def send_hipchat(recipient, sender, release_info)
    begin
      message = <<END_OF_MESSAGE
<b>Build #{release_info[:build_number]} Published</b><br>
<b>Date:</b> #{release_info[:timestamp]}<br>
<b>User:</b> #{release_info[:username]}@#{release_info[:hostname]}<br>
END_OF_MESSAGE

      if release_info[:previous_build_number]
        message += diff(release_info[:artefacts], release_info[:previous_build_number], release_info[:build_number]).gsub('h3', 'b')
      end

      RestClient.post("#{@hipchat_server}/v1/rooms/message?format=json&auth_token=#{@hipchat_token}", :room_id => @hipchat_room, :from => @hipchat_from, :message_format => 'html', :color => 'green', :message => message)

    rescue Exception => e
      puts "HipChat send failed: #{e}"
    end
  end

  def send_email(recipient, sender, release_info)
    html_body = <<END_OF_MESSAGE
    <h2>Build #{release_info[:build_number]} Published</h2>
    <p>
      The following artifact has been cut from branch #{release_info[:git_branch]}:
    </p>

    <table>
      <tr><th>Build</th><td>#{release_info[:build_number]}</td></tr>
      <tr><th>VCS</th><td>#{release_info[:git_url]} #{release_info[:git_branch]} #{release_info[:git_last_commit]}</td></tr>
      <tr><th>Date</th><td>#{release_info[:timestamp]}</td></tr>
      <tr><th>Host</th><td>#{release_info[:hostname]}</td></tr>
      <tr><th>User</th><td>#{release_info[:username]}</td></tr>
    </table>
END_OF_MESSAGE

    if release_info[:artefacts] && !release_info[:artefacts].empty?
      html_body += "<h3>This release includes:</h3><ul>\n"
      release_info[:artefacts].each do |artefact|
        html_body += "<li>#{artefact[:group]}:#{artefact[:artefact]} <i>@ #{version_link(artefact[:jira_group], artefact[:version])}</i></li>\n"
      end
      html_body += "</ul>\n"
    end

    if release_info[:previous_build_number]
      html_body += diff(release_info[:artefacts], release_info[:previous_build_number], release_info[:build_number])
    end

    if release_info[:git_history] && !release_info[:git_history].empty?
      html_body += "<h3>Changes to Support since last release<h3><h4>#{release_info[:git_last_branch]} #{release_info[:git_last_hash]}</h4>\n"
      html_body += "<pre>#{release_info[:git_history]}</pre>"
    end

    boundary = "ohmygodthebees"
    msg = <<END_OF_MESSAGE
From: Build <#{sender}>
To: #{recipient}
Subject: Build #{release_info[:build_number]} created
Date: #{Time.now.strftime('%a, %d %b %Y %H:%M:%S %Z')}
MIME-Version: 1.0
Content-type: multipart/mixed; boundary="#{boundary}"
Content-Transfer-Encoding:8bit
Message-Id: <#{`uuidgen`.strip}@#{hostname}>

You don't support MIME. Sucks to be you.
--#{boundary}
Content-Type: text/html
Content-Transfer-Encoding:8bit

#{html_body}
--#{boundary}--
END_OF_MESSAGE

    begin
      smtp_send(msg, sender, recipient)
    rescue Exception => e
      puts "Email send failed: #{e}"
    end
  end

  def version_link(jira_group, version, colour = nil)
    if jira_group
      jira_host = @config[Yazino::Config::JIRA_HOST]
      "<a #{style(colour)} href=\"http://#{jira_host}/issues/?jql=labels%3D%22#{jira_group}-#{version}%22\">#{version}</a>"
    else
      "<span #{style(colour)}>#{version}</span>"
    end
  end

  def style(colour)
    if colour
      "style=\"color: #{colour}\""
    else
      ""
    end
  end

  def diff(artefacts, old_release, new_release)
    diff = Diff.new(@override_config, true).releases(old_release, new_release)

    html_body = "<h3>Differences from #{diff[:from_name]}</h3>"

    html_body += "<ul style=\"font-family: monospace\">\n"
    diff[:shared].sort.each do |component|
      jira_group = jira_group_for(component, artefacts)
      if diff[:from_release]['versions'][component] != diff[:to_release]['versions'][component]
        html_body += "<li><span style=\"color: blue\">M</span>&nbsp;#{component} @ "
        html_body += "#{version_link(jira_group, diff[:from_release]['versions'][component], 'red')} -&gt; "
        html_body += "#{version_link(jira_group, diff[:to_release]['versions'][component], 'green')}</li>\n"
      else
        html_body += "<li>&nbsp;&nbsp;#{component} @ #{version_link(jira_group, diff[:from_release]['versions'][component])}</li>\n"
      end
    end

    diff[:removed].sort.each do |component|
      jira_group = jira_group_for(component, artefacts)
      html_body += "<li style=\"color: red\">D&nbsp;#{component} @ #{version_link(jira_group, diff[:from_release]['versions'][component], 'red')}</li>\n"
    end

    diff[:new].sort.each do |component|
      jira_group = jira_group_for(component, artefacts)
      html_body += "<li style=\"color: green\">A&nbsp;#{component} @ #{version_link(jira_group, diff[:to_release]['versions'][component], 'green')}</li>\n"
    end
    html_body += "</ul>\n"

    html_body
  end

  def hostname
    Socket.gethostname
  end

  def smtp_send(message, sender, recipient)
      Net::SMTP.start(@smtp_server, @smtp_port, hostname, @smtp_username, @smtp_password, @smtp_auth) do |smtp|
        smtp.send_message(message, sender, recipient)
      end
  end

  def jira_group_for(component, artefacts)
    artefact = artefacts.find {|current_artefact| current_artefact[:artefact] == component}
    if !artefact.nil?
      artefact[:jira_group]
    else
      nil
    end
  end

end
