class LightstreamerListener
  include HandyTableListener

  def onRawUpdatesLost(itemPos, itemName, lostUpdates)
  end

  def onSnapshotEnd(itemPos, itemName)
  end

  def onUnsubscr(itemPos, itemName)
  end

  def onUnsubscrAll
  end

  def onUpdate(itemPos, itemName, update)
    begin
      message_type = update.getNewValue 'contentType'
      puts_with_timestamp "LS Received message #{message_type}"
      message_content = JSON.parse update.getNewValue 'body'
      $semaphore.synchronize do
        handle_next_message message_type, message_content
      end
    rescue => e
      puts_with_timestamp "Update failed: #{e}"
      puts e.backtrace.join "\n\t"
    end
  end
end