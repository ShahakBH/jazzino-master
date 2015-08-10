package strata.server.lobby.controlcentre.repository

import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito.{verify, when}
import org.mockito.Matchers.{eq => equalTo}
import org.springframework.jdbc.core.{RowMapper, JdbcTemplate}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FlatSpec}
import java.util.Arrays.asList
import java.lang.Integer.{valueOf => javaInt}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.{ResultSet, PreparedStatement, Statement, Connection}
import javax.sql.DataSource

@RunWith(classOf[JUnitRunner])
class MySQLPagingTest extends FlatSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {
    val numberOfRows = 20

    val jdbcTemplate = mock[JdbcTemplate]
    val rowMapper = mock[RowMapper[TestModel]]
    val countStmt = mock[Statement]
    val queryStmt = mock[PreparedStatement]
    val connection = mock[Connection]
    val dataSource = mock[DataSource]
    val countResult = mock[ResultSet]
    val queryResult = mock[ResultSet]

    when(jdbcTemplate.getDataSource).thenReturn(dataSource)
    when(dataSource.getConnection).thenReturn(connection)

    when(countResult.next()).thenReturn(true, false)
    when(countResult.getInt(1)).thenReturn(numberOfRows)
    when(countStmt.executeQuery("SELECT FOUND_ROWS()")).thenReturn(countResult)

    when(queryResult.next()).thenReturn(true, true, true, true, true, false)
    when(rowMapper.mapRow(queryResult, 1)).thenReturn(new TestModel(1))
    when(rowMapper.mapRow(queryResult, 2)).thenReturn(new TestModel(2))
    when(rowMapper.mapRow(queryResult, 3)).thenReturn(new TestModel(3))
    when(rowMapper.mapRow(queryResult, 4)).thenReturn(new TestModel(4))
    when(rowMapper.mapRow(queryResult, 5)).thenReturn(new TestModel(5))

    when(connection.createStatement).thenReturn(countStmt)
    when(connection.prepareStatement("SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS * FROM TEST_MODEL LIMIT ?,?")).thenReturn(queryStmt)
    when(queryStmt.executeQuery()).thenReturn(queryResult)

    val underTest = new MySQLPagingTester[TestModel]

    "The MySQL Pager" should "modify the SQL statement to include SQL_CALC_FOUND_ROWS and LIMIT" in {
        underTest.selectWithPaging(jdbcTemplate, "SELECT * FROM TEST_MODEL", rowMapper, 0, 20)

        verify(connection).prepareStatement("SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS * FROM TEST_MODEL LIMIT ?,?")
    }

    it should "invoke the row mapper on each result" in {
        underTest.selectWithPaging(jdbcTemplate, "SELECT * FROM TEST_MODEL", rowMapper, 0, 20)

        verify(rowMapper).mapRow(queryResult, 1)
        verify(rowMapper).mapRow(queryResult, 2)
        verify(rowMapper).mapRow(queryResult, 3)
        verify(rowMapper).mapRow(queryResult, 4)
        verify(rowMapper).mapRow(queryResult, 5)
    }

    it should "pass the given page number and size to the query" in {
        underTest.selectWithPaging(jdbcTemplate, "SELECT * FROM TEST_MODEL", rowMapper, 3, 10)

        verify(queryStmt).setInt(1, 3 * 10)
        verify(queryStmt).setInt(2, 10)
    }

    it should "query MySQL for the full number of rows found" in {
        underTest.selectWithPaging(jdbcTemplate, "SELECT * FROM TEST_MODEL", rowMapper, 0, 20)

        verify(countStmt).executeQuery("SELECT FOUND_ROWS()")
    }

    it should "return the current page of data" in {
        val page = 8
        val pageSize = 5
        val thePageOfData = asList(new TestModel(1), new TestModel(2), new TestModel(3), new TestModel(4), new TestModel(5))

        val pagedData = underTest.selectWithPaging(jdbcTemplate, "SELECT * FROM TEST_MODEL", rowMapper, page, pageSize)

        pagedData.getData should be === thePageOfData
        pagedData.getTotalSize should be === numberOfRows
        pagedData.getSize should be === pageSize
        pagedData.getStartPosition should be === (page * pageSize)
    }

    private[MySQLPagingTest] class MySQLPagingTester[T] extends MySQLPaging[T] {
        override def selectWithPaging(jdbcTemplate: JdbcTemplate, sqlStatement: String, rowMapper: RowMapper[T], page: Int, pageSize: Int) =
            super.selectWithPaging(jdbcTemplate, sqlStatement, rowMapper, page, pageSize)
    }

    private[MySQLPagingTest] case class TestModel(id: Int)

}
