import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor

String targetProject
String key

MutableIssue currentIssue = ComponentAccessor.issueManager.getIssueObject(key)
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def issueFactory = ComponentAccessor.getIssueFactory()
def issueManager = ComponentAccessor.getIssueManager()
MutableIssue newTask = issueFactory.getIssue()

def cfScope = ComponentAccessor.customFieldManager.getCustomFieldObjects()
for (def cf : cfScope){
    if (cf.getValue(currentIssue))
        newTask.setCustomFieldValue(cf,cf.getValue(currentIssue) )        
}
newTask.setSummary(currentIssue.summary)
newTask.setProjectObject(ComponentAccessor.projectManager.getProjectByCurrentKeyIgnoreCase(targetProject))
newTask.setDescription(currentIssue.description)
newTask.setReporterId(currentIssue.reporterId)
def customerRequestType= ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Customer Request Type").first()
switch (targetProject){
    case "A":
        def serviceReqType =  customerRequestType.getCustomFieldType().getSingularObjectFromString("a/getithelp")
        newTask.setIssueTypeId(ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find{it.getName() == "Task"}.id)
        newTask.setCustomFieldValue(customerRequestType,serviceReqType)
        break
    case "B":
        def serviceReqType =  customerRequestType.getCustomFieldType().getSingularObjectFromString("b/submitrequest")
        newTask.setIssueTypeId(ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find{it.getName() == "Service Request"}.id)
        newTask.setCustomFieldValue(customerRequestType,serviceReqType)
        break
    case "c":
        def serviceReqType =  customerRequestType.getCustomFieldType().getSingularObjectFromString("c/changerequest")
        newTask.setIssueTypeId(ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find{it.getName() == "Service Request with Approval"}.id)
        newTask.setCustomFieldValue(customerRequestType,serviceReqType)
        break
}	

Map<String,Object> newIssueParams = ["issue" : newTask] as Map<String,Object>
issueManager.createIssueObject(user, newIssueParams)
ComponentAccessor.commentManager.create(currentIssue, user, "Тикет перемещен в проект ${targetProject} c новым ключом ${newTask.key}", true)
Long linkId = 10100
ComponentAccessor.getIssueLinkManager().createIssueLink(currentIssue.getId(),newTask.getId(),linkId,1,user)
