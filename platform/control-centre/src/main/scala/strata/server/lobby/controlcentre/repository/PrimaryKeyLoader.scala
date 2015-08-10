package strata.server.lobby.controlcentre.repository

import java.lang.{Long => JavaLong}
import org.apache.commons.logging.LogFactory
import org.springframework.jdbc.core.PreparedStatementCallback
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class PrimaryKeyLoader extends PreparedStatementCallback[JavaLong] {
    private val LOG = LogFactory.getLog(classOf[PrimaryKeyLoader])

    def doInPreparedStatement(statement: PreparedStatement): JavaLong = {
        statement.execute
        var rs: ResultSet = null
        try {
            rs = statement.getGeneratedKeys
            if (rs.next) {
                val key: JavaLong = rs.getLong(1)
                LOG.debug("readAggregate key " + key)
                key
            } else {
                null
            }
        }
        finally {
            if (rs != null) {
                try {
                    rs.close()
                }
                catch {
                    case e: SQLException => {
                    }
                }
            }
        }
    }
}
