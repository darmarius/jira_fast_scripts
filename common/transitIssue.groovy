import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.TransitionOptions.Builder

transitIssue(issue,"In progress")

def transitIssue(Issue issue, String transitName){
	ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey("admin");
	def workflow = ComponentAccessor.workflowManager.getWorkflow(issue)
	int transitionID = workflow.getActionsByName(transitName).first().id
	IssueService issueService = ComponentAccessor.getIssueService()
	def builder =  new Builder()
    def transopt = builder.skipConditions().skipValidators().skipPermissions()
	IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issue.getId(), transitionID, issueService.newIssueInputParameters(),transopt.build())
	if (transitionValidationResult.isValid()) 
		IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult)
}