import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Logger
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.fields.ImmutableCustomField

def log = Logger.getLogger("com.acme.workflows")
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey("JIRA_Service")
List<ImmutableCustomField> customFields = (List<ImmutableCustomField>)ComponentAccessor.customFieldManager.getCustomFieldObjects()
List<ImmutableCustomField> uselessCF = new ArrayList()
def mapOfUsage = new HashMap()
customFields = customFields.findAll{it.getCustomFieldType().name !in ["Issue Matrix","Single Issue Picker","Deviniti [Issue Templates] - Template Selection","Approvals","System field"]}
for (ImmutableCustomField cf : customFields){
    //log.warn "Check usage for customfield:  ${cf.getName()}"    cf[11932]
    def jqlForCF  = "cf[${cf.getIdAsLong()}] is not empty" //делаем запрос и получаем колво тикетов которые используют данный cf
    //log.warn jqlForCF
    final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlForCF)
    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    def issuesCount = results.getResults().size()
    //log.warn  "${cf.name} have ${issuesCount} issues"	
    if (issuesCount<10) uselessCF.add("$cf.name($cf.id)")//если cf не используется, то можно считать его бесполезным
    else mapOfUsage.put("$cf.name($cf.id)",issuesCount) 
}
//return uselessCF.size()
StringBuilder body = new StringBuilder()
body = parseListToBody(body,uselessCF,"Useless Custom Fields")
body.append("\n")
body.append("----------------------------------------------------")
body.append("Usage of Custom Fields")
body.append("----------------------------------------------------")
body.append("\n")
mapOfUsage.sort{it.value}.each{K,V->    
    if (V>0){
    	body.append("${K} have ${V} issues. ")
        body.append("\n")
    }
}
return body.toString()
def jiraHome = ComponentAccessor.getComponent(JiraHome)
def  file = new File(jiraHome.home, "CF_analyze.txt") //location JIRA_HOME/CF_analyze.txt

file.write body.toString()
return file.readLines()

StringBuilder parseListToBody (StringBuilder body,def source, String name){
	body.append("\n")
	body.append("----------------------------------------------------")
	body.append(name)
	body.append("----------------------------------------------------")
	body.append("\n")
	def counter = 0
	source.each{
		body.append(it)
		body.append(", ")
		counter++
		if (counter == 5){
			counter=0
			body.append("\n")
		}        	
	}
	body.append("\n")
	body.append("-----------------------------------------------------------------------------------------------------------------------------------")
	body.append("\n")
	return body
}