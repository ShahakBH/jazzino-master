$LOAD_PATH << File.dirname(__FILE__)
$LOAD_PATH << File.expand_path('../../lib/', __FILE__)

require 'test_helper'
require 'method/web_load_balancer'

class WebLoadBalancerTest < Test::Unit::TestCase

  def setup
    WebLoadBalancer.any_instance.stubs(:sleep).returns 0
    WebLoadBalancer.any_instance.stubs(:remove_from_load_balancer).returns 0
    WebLoadBalancer.any_instance.stubs(:add_to_load_balancer).returns 0
    @host = "myhost"
    @wlb = WebLoadBalancer.new({})
    @wlb_check = WebLoadBalancerCheck.new({})
  end

  def test_removed_succeeds
    WebLoadBalancer.any_instance.stubs(:is_available).returns(true, true, false)
    assert_nothing_raised RuntimeError do
      @wlb.remove(@host)
    end
  end

  def test_removed_fails
    codes = []
    15.times { codes << true }
    WebLoadBalancer.any_instance.stubs(:is_available).returns(*codes)
    assert_raise RuntimeError do
      @wlb.remove(@host)
    end
  end

  def test_added
    WebLoadBalancer.any_instance.stubs(:is_available).returns(false, false, true)
    assert_nothing_raised RuntimeError do
      @wlb.wait_until_added(@host)
    end
  end

  def test_added_fails
    codes = []
    15.times { codes << false }
    WebLoadBalancer.any_instance.stubs(:is_available).returns(*codes)
    assert_raise RuntimeError do
      @wlb.wait_until_added(@host)
    end
  end

  def test_unavailable_code_000
    @wlb_check.expects(:run_curl).returns("000")
    assert_false @wlb_check.is_available(@host)
  end

  def test_unavailable_status_not_ok
    @wlb_check.expects(:run_curl).returns("{\n  \"status\": \"grid-error\"\n}\n200")
    assert_false @wlb_check.is_available(@host)
  end

  def test_unavailable_code_404
    @wlb_check.expects(:run_curl).returns("{\npage not found\n}\n404")
    assert_false @wlb_check.is_available(@host)
  end

  def test_available
    @wlb_check.expects(:run_curl).returns("{\n  \"status\": \"okay\"\n}\n200")
    assert_true @wlb_check.is_available(@host)
  end
end
