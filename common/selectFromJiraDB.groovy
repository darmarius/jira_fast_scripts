import com.atlassian.jira.component.ComponentAccessor
import groovy.sql.Sql
import java.sql.Connection
import org.ofbiz.core.entity.ConnectionFactory
import org.ofbiz.core.entity.DelegatorInterface

String helperName = ComponentAccessor.getComponent(DelegatorInterface).getGroupHelperName("default")
StringBuffer sb = new StringBuffer()
def sqlStmt = """
  SELECT i.id, CONCAT(p.pkey,'-',issuenum) as issuekey,i.summary,it.pname as issuetype,s.pname as status FROM jiraissue as i 
  JOIN project as p ON p.id = i.PROJECT
  JOIN issuetype as it ON it.ID = i.issuetype
  JOIN issuestatus as s ON s.ID = i.issuestatus
  WHERE p.pkey = 'TEST' and it.pname in ('New Feature','Task')
  ORDER BY i.issuenum DESC
"""
Connection conn = ConnectionFactory.getConnection(helperName)
Sql sql = new Sql(conn)
try {
    sql.eachRow(sqlStmt) {
        log.info("${it.id} ${it.issuekey} ${it.status}")
    }
}
finally {
    sql.close()
}