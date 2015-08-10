require 'method/deployment_method'

class NoOpMethod < DeploymentMethod

  def initialize(config, artefact)
    super(config, artefact)
  end

end
