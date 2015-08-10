$LOAD_PATH << File.dirname(__FILE__)
$LOAD_PATH << File.expand_path('../../lib/', __FILE__)

require 'test_helper'
require 'deploy'

class DeploymentTest < Test::Unit::TestCase

  def setup
    @staging_box = ['rck-bmc-mon1.breakmycasino.com']
    @lobby_boxes = [
        'rck-bmc-lobby1.breakmycasino.com',
        'rck-bmc-lobby2.breakmycasino.com'
    ]
  end

  def test_basic_web_deployment
    checker = DeploymentChecker.new self
    checker.staged_to @staging_box
    checker.maintenance_on
    checker.jetty_stopped_on @lobby_boxes
    checker.deployed_web_to @lobby_boxes
    checker.maintenance_off

    deploy = Yazino::Deploy.new(["web"], {:deploy => "dev", :skip_chef => true})
    deploy.run(Yazino::Config.load("breakmycasino"))
  end

  def test_live_web_deployment
    checker = DeploymentChecker.new self
    checker.staged_to @staging_box

    @lobby_boxes.each do |lobby|
      checker.removed_from_load_balancer lobby
      checker.jetty_stopped_on [lobby]
      checker.deployed_web_to [lobby]
      checker.added_to_load_balancer lobby
    end

    deploy = Yazino::Deploy.new(["web"], {:deploy => "dev", :live => true, :verbose => true, :skip_chef => true})
    deploy.run(Yazino::Config.load("breakmycasino"))
  end
end
