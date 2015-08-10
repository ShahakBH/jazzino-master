module ThreadedTask

  def wait_for_threads(task_name, timeout, threads)
    threads.each do |thread|
      if thread.join(timeout).nil?
        backtrace = thread.backtrace
        status = thread.status
        thread.kill

        yield(thread) if block_given?

        thread_name = task_name
        thread_name += " (#{thread[:name]})" if thread[:name]
        raise "#{self.class.name}: #{thread_name} timed out; status was #{status}; trace is #{backtrace}"
      end
    end
  end

end
