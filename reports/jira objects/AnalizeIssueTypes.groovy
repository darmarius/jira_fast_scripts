import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Logger
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.fields.screen.DefaultFieldScreenSchemeManager
import com.atlassian.jira.issue.fields.screen.FieldScreen
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme

def issueTypeScreenSchemesAll = ComponentAccessor.getIssueTypeScreenSchemeManager().getIssueTypeScreenSchemes()
def goodIssueTypes = new ArrayList()
List<FieldScreenScheme> usedScreenSchemes = new ArrayList()
for (def itss : issueTypeScreenSchemesAll){
	if(itss.getProjects()) {
        itss.getEntities().each{ entity->
            if(entity.getIssueType() && !goodIssueTypes.find{it==entity.getIssueType().getName()}) goodIssueTypes.add(entity.getIssueType().getName())
        }    
    }
}
for (def project : ComponentAccessor.projectManager.getProjectObjects()){
	project.getIssueTypes().each{ issuetype->
    	if(!goodIssueTypes.find{it==issuetype.getName()}) goodIssueTypes.add(issuetype.getName())
    }    
}
def mapOfUsageIssueTypes = new HashMap() 
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey("JIRA_Service")
for (def it : goodIssueTypes){
    def jqlForWF = "issuetype=${it}"
    //log.warn jqlForWF
    final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlForWF)
    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    def issuesCount = results.getResults().size()
    //log.warn  "${wf.name} have ${issuesCount} issues"	
	mapOfUsageIssueTypes.put(it,issuesCount)
}

StringBuilder body = new StringBuilder()
body.append("\n----------------------------------------------------")
body.append("Usage of good workflows")
body.append("----------------------------------------------------\n")
mapOfUsageIssueTypes.sort{it.value}.sort().each{K,V->    
    	body.append("${K} have ${V} issues. \n")
}


return body.toString()

def jiraHome = ComponentAccessor.getComponent(JiraHome)
def file = new File(jiraHome.home, "issue_type_analize.txt") //location JIRA_HOME/issue_type_analize.txt
file.write body.toString()

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