import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor

def user = ComponentAccessor.getUserManager().getUserByName("Admin")
def issueFactory = ComponentAccessor.getIssueFactory()
def issueManager = ComponentAccessor.getIssueManager()
MutableIssue newTask = issueFactory.getIssue()
newTask.setSummary("summary")
newTask.setProjectObject(ComponentAccessor.projectManager.getProjectByCurrentKeyIgnoreCase("TEST"))
newTask.setDescription("desc")
newTask.setReporter(user)
newTask.setAssigneeId("username")
newTask.setIssueTypeId(ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find{it.getName() == "Service Request"}.id)
def cf = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("CF name").first()
newTask.setCustomFieldValue(cf,"value")

Map<String,Object> newIssueParams = ["issue" : newTask] as Map<String,Object>
issueManager.createIssueObject(user, newIssueParams)