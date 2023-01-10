import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.issue.link.RemoteIssueLinkManager
import com.atlassian.servicedesk.api.requesttype.RequestTypeService
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import org.apache.log4j.Logger
import org.apache.log4j.Level

@WithPlugin("com.atlassian.servicedesk")
@PluginModule
RequestTypeService requestTypeService
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def reqQ = requestTypeService.newQueryBuilder().issue(issue.id).build()
def reqT = requestTypeService.getRequestTypes(currentUser, reqQ)
def requestType = reqT.results[0].getName() //Получаем название услуги
def customFields = ComponentAccessor.customFieldManager.getCustomFieldObjects()
if (requestType.equals("Confluence space management")){ 
    log.debug("${issue.key} Script started")
    def linksCF = customFields.find {it.getFieldName().equals("Links")}
	String stringlinks =  issue.getCustomFieldValue(linksCF)    
	IssueService issueService = ComponentAccessor.getIssueService()
    def user = ComponentAccessor.userManager.getUserByKey("userkey")
	def validateAssignResult = issueService.validateAssign(user, issue.id, "userkey")
	issueService.assign(user, validateAssignResult)
    log.debug("${issue.key} assigned to 1st Level")
    if (stringlinks){
        def links = stringlinks.split()
    	int count =0
    	for (slink in links){
			if(slink != ""){
				try{
					def linkBuilder = new RemoteIssueLinkBuilder()
					linkBuilder.issueId(issue.id)
					linkBuilder.relationship("EX")
					linkBuilder.title("Example Space")
					linkBuilder.url(slink)
					def link = linkBuilder.build() 
					ComponentAccessor.getComponent(RemoteIssueLinkManager).createRemoteIssueLink(link, user)
                	count++
					}
            	catch (Exception ex) {}
			}
		}
        log.debug("${issue.key} Script finished, links added:${count}")
    }
    else log.debug("${issue.key} There is no links")
}
else log.debug("This issue not 'Confluence space management'")