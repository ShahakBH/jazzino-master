class BotConnectionListener
  include ExtendedConnectionListener

  def onConnectionEstablished
    puts_with_timestamp "LS Connection established"
  end

  def onSessionStarted(isPolling, controlLink = nil)
    puts_with_timestamp "LS Session Started"
  end

  def onNewBytes(newBytes)
  end

  def onDataError(e)
    puts_with_timestamp "Data Error: #{e}"
  end

  def onActivityWarning(warningOn)
  end

  def onEnd(cause)
    puts_with_timestamp "LS End: #{cause}"
  end

  def onClose
    puts_with_timestamp "LS Closed"
  end

  def onFailure(e)
    puts_with_timestamp "LS Failure: #{e}"
  end
end
