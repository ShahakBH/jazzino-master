class Web < Sinatra::Base
  configure {
    set :server, :puma
    set :bind, '0.0.0.0'
    set :port, 3000
  }
  get '/' do
    $stats.to_json
  end
end
