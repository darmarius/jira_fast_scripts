import com.atlassian.jira.component.ComponentAccessor
import groovy.sql.Sql
import java.sql.Connection
import org.ofbiz.core.entity.ConnectionFactory
import org.ofbiz.core.entity.DelegatorInterface

String query = """
SELECT destination, count(*), count(distinct source) 
FROM issuelink 
WHERE linktype = 10300
GROUP BY destination having count(*) > 1
"""
selectFromJiraDB(query)

StringBuffer selectFromJiraDB(String query){
    String helperName = ComponentAccessor.getComponent(DelegatorInterface).getGroupHelperName("default")
    StringBuffer sb = new StringBuffer()
    Connection conn = ConnectionFactory.getConnection(helperName)
    Sql sql = new Sql(conn)
    try {
        sql.eachRow(query) {
            sb.append("<p>${it}</p>")
        }
    }
    finally {
        sql.close()
    }
    return sb
}
