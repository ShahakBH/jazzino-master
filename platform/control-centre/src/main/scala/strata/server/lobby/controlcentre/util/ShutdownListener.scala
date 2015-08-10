package strata.server.lobby.controlcentre.util

import org.slf4j.LoggerFactory
import javax.servlet.{ServletContextEvent, ServletContextListener}
import org.springframework.context.ConfigurableApplicationContext
import com.gigaspaces.lrmi.classloading.protocol.lrmi.LRMIConnection
import java.sql.DriverManager
import org.springframework.web.context.ContextLoader

class ShutdownListener extends ServletContextListener {
    private val LOG = LoggerFactory.getLogger(classOf[ShutdownListener])

    def contextDestroyed(event: ServletContextEvent) {
        val bf = currentApplicationContext()
        if (bf.isInstanceOf[ConfigurableApplicationContext] )
        {
            val context = bf.asInstanceOf[ConfigurableApplicationContext]

            shutdownGigaSpace()

            context.close()

            deregisterDrivers()
        }
    }

    private def shutdownGigaSpace() {
        try {
            LRMIConnection.clearConnection()
        } catch {
            case e: Exception => {
                LOG.error("Failed to shutdown LRMI Connection", e)
            }
        }
    }

    private def deregisterDrivers() {
        val drivers = DriverManager.getDrivers
        while (drivers.hasMoreElements) {
            val driver = drivers.nextElement()
            try {
                DriverManager.deregisterDriver(driver)
            } catch {
                case e: Exception => {
                    LOG.error("Failed to de-register driver: " + driver, e)
                }
            }
        }
    }

    def contextInitialized(event: ServletContextEvent) {
    }

    private def currentApplicationContext() = {
        ContextLoader.getCurrentWebApplicationContext
    }

}
