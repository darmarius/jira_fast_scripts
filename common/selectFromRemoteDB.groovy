import com.valiantys.jira.plugins.sql.*
import groovy.sql.Sql
import java.sql.Driver
import groovy.sql.GroovyRowResult


String GetSomeData() {
    String query = """SELECT data as value from table"""
    List<GroovyRowResult> rows = ExecuteRequest(query)
    return rows.toString().replaceAll('[{|}:|"|\\[|\\]]',"").replace("value","")
}

List<GroovyRowResult> ExecuteRequest(String query) {
    def driver = Class.forName('net.sourceforge.jtds.jdbc.Driver').newInstance() as Driver
    def props = new Properties()
    props.setProperty("user", "user")
    props.setProperty("password", "password")
    props.setProperty("autocommit", "false")
    def conn = driver.connect("jdbc:string", props)
    conn.setAutoCommit(false)
    List<GroovyRowResult> result = null
    def sql = new Sql(conn)
    try {
        result = sql.rows(query)
    } finally {
        sql.close()
        conn.close()
    }
    return result
}