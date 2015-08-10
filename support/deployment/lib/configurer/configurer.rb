require 'threaded_task'

class Configurer
  include ThreadedTask

  def run(step, components, options = {})
    self.send(step, components, options) if self.class.method_defined?(step)
  end

end
