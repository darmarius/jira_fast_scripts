import com.atlassian.jira.component.ComponentAccessor
import groovy.sql.Sql
import org.ofbiz.core.entity.ConnectionFactory
import org.ofbiz.core.entity.DelegatorInterface
import com.atlassian.jira.config.util.JiraHome

import java.sql.Connection

String helperName = ComponentAccessor.getComponent(DelegatorInterface).getGroupHelperName("default")
StringBuffer sb = new StringBuffer()
String badFiltersQuery = """
SELECT id,filtername, authorname
FROM searchrequest
WHERE authorname in (select user_name from cwd_user where active=0) 
AND id not in (SELECT filter_i_d FROM filtersubscription);
"""
Connection conn = ConnectionFactory.getConnection(helperName)
Sql sql = new Sql(conn)

try {
    //BAD FILTERS
    sb.append("""
    <p>BAD FILTERS</p>
	<table border="1"><tbody>
		<tr style="background-color:#8a8780; font-weight: bold;">
			<td>ID</td>
			<td>filtername</td>
			<td>authorname</td>
		</tr> """)
    sql.eachRow(badFiltersQuery) {
        sb.append("""
		<tr>
			<td>${it.id}</td>
			<td>${it.filtername}</td>
            <td>${it.authorname}</td>
		</tr>""")
    }
    sb.append("</tbody></table>")  
	
	/*GOOD FILTERS
	String goodFilters = """
	SELECT id,filtername, authorname
	FROM searchrequest
	WHERE authorname in (select user_name from cwd_user where active=1) 
	OR id in (SELECT filter_i_d FROM filtersubscription);
	"""
    sb.append("""
    <p>Good Filters</p>
	<table border="1"><tbody>
		<tr style="background-color:#8a8780; font-weight: bold;">
			<td>ID</td>
			<td>filtername</td>
			<td>authorname</td>
		</tr> """)
    sql.eachRow(goodFilters) {
        sb.append("""
		<tr>
			<td>${it.id}</td>
			<td>${it.filtername}</td>
        	<td>${it.authorname}</td>
		</tr>""")
    }

    sb.append("</tbody></table>")  */
	//GADGETS FOR DELETE
    String badGadgetsQuery = """
	SELECT id,portletconfiguration, userprefvalue
	FROM gadgetuserpreference
	WHERE userprefkey='filterId'
	AND REPLACE(CAST(userprefvalue as VARCHAR(255)),'filter-','') NOT IN (
		SELECT CAST(id as VARCHAR(255))
		FROM searchrequest
		WHERE authorname in (select user_name from cwd_user where active=1) 
		OR id in (SELECT filter_i_d FROM filtersubscription)
	);
	"""
    sb.append("""
	<p>Gadgets for Delete</p>
    <table border="1"><tbody>
		<tr style="background-color:#8a8780; font-weight: bold;">
			<td>ID</td>
			<td>Place on dashboard</td>
            <td>Broken Filter id</td>
		</tr> """)
    sql.eachRow(badGadgetsQuery) {
        sb.append("""
		<tr>
			<td>${it.id}</td>
			<td>${it.portletconfiguration}</td>
            <td>${it.userprefvalue}</td>
		</tr>""")
    }
    sb.append("</tbody></table>")  
    //Dashboards with bad gadgets
	String badGadgetsDashboardQuery = """
	SELECT portalpage,gadget_xml
	FROM portletconfiguration
	WHERE id in (
		SELECT portletconfiguration
		FROM gadgetuserpreference
		WHERE userprefkey='filterId'
		AND REPLACE(CAST(userprefvalue as VARCHAR(255)),'filter-','') NOT IN (
			SELECT CAST(id as VARCHAR(255))
			FROM searchrequest
			WHERE authorname in (select user_name from cwd_user where active=1) 
			OR id in (SELECT filter_i_d FROM filtersubscription)
		)
	);
	"""
    sb.append("""
	<p>Gadgets for Delete from Dashboards</p>
    <table border="1"><tbody>
		<tr style="background-color:#8a8780; font-weight: bold;">
			<td>Dashboard ID</td>
            <td>gadget_xml</td>
		</tr> """)
    sql.eachRow(badGadgetsDashboardQuery) {
        sb.append("""
		<tr>
			<td>${it.portalpage}</td>
			<td>${it.gadget_xml}</td>
		</tr>""")
    }
    sb.append("</tbody></table>")  
	//BAD DASHBOARD
    String badDashboardsQuery = """
	SELECT id, username, pagename
	FROM portalpage
	WHERE id not in (SELECT portalpage FROM portletconfiguration)
    OR id in (
		SELECT portalpage
		FROM portletconfiguration
		WHERE id in (
			SELECT portletconfiguration
			FROM gadgetuserpreference
			WHERE userprefkey='filterId'
			AND REPLACE(CAST(userprefvalue as VARCHAR(255)),'filter-','') NOT IN (
				SELECT CAST(id as VARCHAR(255))
				FROM searchrequest
				WHERE authorname in (select user_name from cwd_user where active=1) 
				OR id in (SELECT filter_i_d FROM filtersubscription)
			)
		)
	);
	"""
	/*"""
	SELECT id, username, pagename
	FROM portalpage
	WHERE id not in (SELECT portalpage FROM portletconfiguration)
    OR id in (SELECT portalpage
	FROM portletconfiguration
	WHERE id in (SELECT portletconfiguration
	FROM gadgetuserpreference
	WHERE userprefkey='filterId'
	AND REPLACE(CAST(userprefvalue as VARCHAR(255)),'filter-','') NOT IN (SELECT CAST(id as VARCHAR(255))
	FROM searchrequest
	WHERE authorname in (select user_name from cwd_user where active=1) 
	OR id in (SELECT filter_i_d FROM filtersubscription))));
	"""*/
    sb.append("""
	<p>Dashboards for Delete</p>
    <table border="1"><tbody>
		<tr style="background-color:#8a8780; font-weight: bold;">
			<td>ID</td>
			<td>NAME</td>
            <td>OWNER</td>
		</tr> """)
    sql.eachRow(badDashboardsQuery) {
        sb.append("""
		<tr>
			<td>${it.id}</td>
			<td>${it.pagename}</td>
            <td>${it.username}</td>
		</tr>""")
    }
    sb.append("</tbody></table>")  
	//BAD RAPID BOARDS
    String badRapidboardsQuery = """
	SELECT "ID", "NAME", "SAVED_FILTER_ID"
	FROM "AO_60DB71_RAPIDVIEW"
	WHERE "SAVED_FILTER_ID" in (SELECT id 
	FROM searchrequest
	WHERE authorname in (select user_name from cwd_user where active=0) 
	AND id not in (SELECT filter_i_d FROM filtersubscription))
	OR "SAVED_FILTER_ID" not in (SELECT id FROM searchrequest);
	"""
    sb.append("""
	<p>Rapid Boards for Delete</p>
    <table border="1"><tbody>
		<tr style="background-color:#8a8780; font-weight: bold;">
			<td>ID</td>
			<td>NAME</td>
            <td>Filter id</td>
		</tr> """)
    sql.eachRow(badRapidboardsQuery) {
        sb.append("""
		<tr>
			<td>${it.ID}</td>
			<td>${it.NAME}</td>
            <td>${it.SAVED_FILTER_ID}</td>
		</tr>""")
    }
    sb.append("</tbody></table>")  
}
finally {
    sql.close()
}

return sb.toString()

def jiraHome = ComponentAccessor.getComponent(JiraHome)
def file = new File(jiraHome.home, "filter_boards_analyze.html") //location JIRA_HOME/filter_boards_analyze.html
file.write sb.toString()