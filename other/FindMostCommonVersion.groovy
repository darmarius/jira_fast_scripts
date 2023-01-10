import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.project.Project
import com.atlassian.jira.security.JiraAuthenticationContext
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*  Description:
    В проекте ищется наиболее похожая меньшая завершенная версия.
    Сначала ищется по первым двум частям (1.1.х.y), в случае неудачи по первой части (1.х.y.z). 
    Если не удается найти похожую версию, то за источник береться последняя завершенная версия ниже текущей.
*/

public class Class_CopyCfFromMostCommonVersion {
    Logger log = Logger.getLogger("MostCommonVersion")
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
    JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext()

    ApplicationUser applicationUser = ComponentAccessor.userManager.getUserByName("jira_service")

    public void Run(MutableIssue issue) {
        log.setLevel(Level.DEBUG)
        long ScriptStartTime = System.currentTimeMillis()
        log.debug("${issue.key} script started")
		
        log.debug("Current version ${issue.fixVersions.first()}")
       	String[] sourceVersionParts = issue.fixVersions.first().toString().split("[.]")
		SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class)
        String jqlForWF ="project = ${issue.getProjectObject().getKey()} and status = Closed"
		final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlForWF)
        final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
        def issues = results.getIssues()
        if (issues) {
            log.debug("Avaiable fixVersions ${issues.fixVersions}")
            Issue latestVersionIssue
            Collection latestVersion = [0L,0L,0L]
            String mostCommonVersionLatestPart
            Issue mostCommonIssue
            //finding most common version by 2 parts
            for (def closedIssue : issues){
                def closedVersionParts = closedIssue.fixVersions.first().toString().split("[.]")                
                if (sourceVersionParts[0]==closedVersionParts[0] && sourceVersionParts[1]==closedVersionParts[1] 
                    && Long.parseLong(sourceVersionParts[2]) > Long.parseLong(closedVersionParts[2])){
                    if (!mostCommonIssue){
                        mostCommonVersionLatestPart = closedVersionParts[2]
                        mostCommonIssue = closedIssue
                    }
                    else if (Long.parseLong(mostCommonVersionLatestPart) < Long.parseLong(closedVersionParts[2])){
                        mostCommonVersionLatestPart = closedVersionParts[2]
                        mostCommonIssue = closedIssue
                    }
                }        
            }
            if (mostCommonIssue){
                log.debug ("Most common version by 2 parts for ${issue.fixVersions.first()} is ${mostCommonIssue.fixVersions.first()}")
                setTechnology(issue, mostCommonIssue, applicationUser)
            }
            else {
            //finding most common version by 1 part
                for (def closedIssue : issues){
                    def closedVersionParts = closedIssue.fixVersions.first().toString().split("[.]")
                    if (sourceVersionParts[0]==closedVersionParts[0] && Long.parseLong(sourceVersionParts[1]) > Long.parseLong(closedVersionParts[1])){
                        if (!mostCommonIssue){
                            mostCommonVersionLatestPart = closedVersionParts[1]
                            mostCommonIssue = closedIssue
                        }
                        else if (Long.parseLong(mostCommonVersionLatestPart) < Long.parseLong(closedVersionParts[1])){
                            mostCommonVersionLatestPart = closedVersionParts[1]
                            mostCommonIssue = closedIssue
                        }
                    }            
                }
                if (mostCommonIssue){
                    log.debug ("Most common version by 1 part for ${issue.fixVersions.first()} is ${mostCommonIssue.fixVersions.first()}")
                    setTechnology(issue, mostCommonIssue, applicationUser)
                }
                //finding latest version
                else{
                    for (def closedIssue : issues){
                        def closedVersionParts = closedIssue.fixVersions.first().toString().split("[.]")
                        if (latestVersion[0]< Long.parseLong(closedVersionParts[0])){
                            latestVersionIssue = closedIssue
                            latestVersion[0] = Long.parseLong(closedVersionParts[0])
                            latestVersion[1] = Long.parseLong(closedVersionParts[1])
                            latestVersion[2] = Long.parseLong(closedVersionParts[2])
                        }
                        else if (latestVersion[0] == Long.parseLong(closedVersionParts[0])){
                            if (latestVersion[1]< Long.parseLong(closedVersionParts[1])){
                                latestVersionIssue = closedIssue
                                latestVersion[2] = Long.parseLong(closedVersionParts[2])
                                latestVersion[1] = Long.parseLong(closedVersionParts[1])
                            }
                            else if (latestVersion[1] == Long.parseLong(closedVersionParts[1])){
                                if (latestVersion[2]< Long.parseLong(closedVersionParts[2])){
                                    latestVersionIssue = closedIssue
                                    latestVersion[2] = Long.parseLong(closedVersionParts[2])
                                }
                            }
                        }           
                    }
                    log.debug ("Most latest version ${latestVersionIssue.fixVersions.first()}")
                }
            }
        }
        long ScriptWorkTime = System.currentTimeMillis() - ScriptStartTime
        log.debug("${issue.key} CopyCfFromMostCommonVersion Script work time: ${ScriptWorkTime} ms.")
    }
}