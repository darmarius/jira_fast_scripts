import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.mail.Email

def applicationUser = ComponentAccessor.getUserManager().getUserByName("jiraadmin")
def group = ComponentAccessor.groupManager.getUsersInGroup("unit_at")
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
StringBuilder body = new StringBuilder()
def fontStyle = """TEXT-DECORATION: none; FONT-FAMILY: 'Segoe UI'; COLOR: #666666; FONT-STYLE: normal; """
def borderCell ="border-width:1px; border-style: solid;  border-color:#dbd9d9;"
def	tableStyle = """cellspacing='0' style=" table-layout: fixed; ${borderCell} ${fontStyle}" """

body.append("""<div>
					<h3 style="${fontStyle} text-align:center;">Группа: обращения, открытые более 60 дней </h3>
					<div>
					<table ${tableStyle}>
						<tbody>
							<tr style=" font-weight: bold;">
								<td style="${borderCell}">Responsible</td>
								<td style="${borderCell}">Number</td>
								<td style="${borderCell}">Creation date</td>
								<td style="${borderCell}">Days</td>
								<td style="${borderCell}">Title</td>
								<td style="${borderCell}">State</td>
							</tr>""")
def total = 0
for (def user : group){
    def jqlForWF ="project!= OA and created < -60d and statusCategory != Done and assignee = ${user.getName()}"
	final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlForWF)
	final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
	def issues = results.getResults()
    if (issues) {
        body.append("""<tr><td rowspan="${issues.size()+1}" style="${borderCell}">${user.getDisplayName()}</td>""")
    	def counter = 0 
    	for (def issue : issues){
            if (counter!=0) body.append("<tr>")
            def key = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") +"/browse/" +issue.key
        	body.append("""
							<td style="${borderCell}"><a title="${issue.key}" href="${key}">${issue.key}</a></td>
							<td style="${borderCell}">${issue.created.getDateString()}</td>
							<td style="${borderCell}">${new Date() - issue.created}</td>
							<td style="${borderCell}">${issue.summary}</td>
							<td style="${borderCell}">${issue.status.getName()}</td>

					</tr>""")
            counter++
    	}
        body.append("""<tr style=" font-weight: bold;">
        				<td colspan="5" style="${borderCell}">${issues.size()}</td>
                        </tr>""")
        total +=  issues.size()
    }
}
body.append("""<tr style=" font-weight: bold;">
						<td style="${borderCell}">Total</td>
        				<td colspan="5" style="${borderCell}">${total}</td>
                        </tr>""")
body.append("</tbody></table></div></div>")

return body