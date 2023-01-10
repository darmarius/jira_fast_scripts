import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.atlassian.servicedesk.api.approval.ApprovalService

def issue = ComponentAccessor.getIssueManager().getIssueObject("SD-1")

@WithPlugin("com.atlassian.servicedesk")

ApprovalService approvalService = ComponentAccessor.getOSGiComponentInstanceOfType(ApprovalService)
def approvalsCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Approvals").first()
def approvals = approvalsCF.getValue(issue).getApprovals()
List<String> approvalsNeeded = approvalService.getApprovers(currentUser, approvals.first()).findAll{!it.getApproverDecision().isPresent()}
    .collect{it.getApproverUser().getEmailAddress()}