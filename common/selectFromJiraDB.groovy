import com.atlassian.jira.component.ComponentAccessor
import groovy.sql.Sql
import java.sql.Connection
import org.ofbiz.core.entity.ConnectionFactory
import org.ofbiz.core.entity.DelegatorInterface

String helperName = ComponentAccessor.getComponent(DelegatorInterface).getGroupHelperName("default")
StringBuffer sb = new StringBuffer()
def sqlStmt = """
select destination, count(*), count(distinct source) from issuelink where linktype = 10300
group by destination having count(*) > 1

"""
Connection conn = ConnectionFactory.getConnection(helperName)
Sql sql = new Sql(conn)
try {
    sql.eachRow(sqlStmt) {
        sb.append("<p>${it}</p>")
    }
}
finally {
    sql.close()
}
return sb