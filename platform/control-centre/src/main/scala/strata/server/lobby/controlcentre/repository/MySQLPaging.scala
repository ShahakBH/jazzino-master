package strata.server.lobby.controlcentre.repository

import com.yazino.platform.model.PagedData
import org.springframework.jdbc.core.{RowMapper, JdbcTemplate}
import java.util
import java.sql.{Connection, ResultSet, PreparedStatement}
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.jdbc.support.JdbcUtils
import javax.sql.DataSource

trait MySQLPaging[T] {

    // You must call this in a transaction if you wish it to work reliably
    protected def selectWithPaging(jdbcTemplate: JdbcTemplate,
                                   sqlStatement: String,
                                   rowMapper: RowMapper[T],
                                   page: Int,
                                   pageSize: Int): PagedData[T] = {
        val modifiedSql = sqlStatement.replaceFirst(
            "^\\s*SELECT\\s", "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS ") + " LIMIT ?,?"

        // we use direct JDBC here to guarantee that
        // a) it all happens on the same connection
        // b) found_rows() is executed immediately after the original select
        // Otherwise, row mappers that query other information can cause this to all go tits up

        var preparedStmt: PreparedStatement = null
        var queryResults: ResultSet = null
        var countResults: ResultSet = null
        var connection: Connection = null
        val dataSource: DataSource = jdbcTemplate.getDataSource
        try {
            connection = DataSourceUtils.getConnection(jdbcTemplate.getDataSource)
            preparedStmt = connection.prepareStatement(modifiedSql)
            preparedStmt.setInt(1, page * pageSize)
            preparedStmt.setInt(2, pageSize)
            queryResults = preparedStmt.executeQuery()

            countResults = connection.createStatement().executeQuery("SELECT FOUND_ROWS()")
            val totalSize = if (countResults.next()) {
                countResults.getInt(1)
            } else {
                0
            }

            var rowNumber = 1
            val results = new util.ArrayList[T]()
            while (queryResults.next()) {
                results.add(rowMapper.mapRow(queryResults, rowNumber))
                rowNumber += 1
            }
            new PagedData[T](page * pageSize, pageSize, totalSize, results)

        } finally {
            JdbcUtils.closeResultSet(queryResults)
            JdbcUtils.closeResultSet(countResults)
            JdbcUtils.closeStatement(preparedStmt)

            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }

}
