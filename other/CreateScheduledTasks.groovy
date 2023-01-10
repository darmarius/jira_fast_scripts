import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.crowd.embedded.api.Group
import org.apache.log4j.Level
import org.apache.log4j.Logger

Logger log = Logger.getLogger("ScheduledIssues")
log.setLevel(Level.DEBUG)

ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName("admin")
def employeesCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Employees for tasks").first()
def groupCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Group for tasks").first()
def timeCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Task Frequency").first()
String timeValue =  issue.getCustomFieldValue(timeCF)
Date dueDate = timeToCreate(timeValue)
if (dueDate){
    log.debug("${issue} ${timeValue} out date ${dueDate}")
	Collection<ApplicationUser> employeeValue
	if (issue.getCustomFieldValue(employeesCF)) 
		employeeValue = (Collection<ApplicationUser>) issue.getCustomFieldValue(employeesCF)
	if (groupCF.getValue(issue)){
    	Collection<ApplicationUser> groupMembers = ComponentAccessor.groupManager.getUsersInGroup((Group)groupCF.getValue(issue)[0])
    	if (employeeValue){
       		employeeValue.addAll(groupMembers)
            employeeValue.unique()
        }
        else employeeValue = groupMembers
	}
    log.debug("List of users: ${employeeValue}")
    createIssues(issue,employeeValue, applicationUser, dueDate)
}

Date timeToCreate(String time){
    Date today = new Date()
    Date dueDate = new Date()
    Calendar cal = Calendar.getInstance()
    boolean updated = false
    switch(time){
        case'Ежедневно':
        	if (today.getDay()==6)
        		dueDate = dueDate.minus(1)
        	if (today.getDay()==0)
        		dueDate = dueDate.minus(2)
            updated = true
        	break
        case'Еженедельно':
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()+1)
        	dueDate = cal.getTime().plus(6)
        	updated = true
        	break
        case'Раз в 2 недели':
        	cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()+1)
        	def createdDate = new Date(issue.created.getDateString())
			def startWeek = createdDate.minus(createdDate.getDay()-1)
			if (((cal.getTime() - startWeek)%14)==0)
    			dueDate = cal.getTime().plus(13)
			else 
                dueDate = cal.getTime().plus(6)
            updated = true
        	break
        case'Ежемесячно':
        	cal.set(Calendar.DAY_OF_MONTH, 1)
            dueDate = cal.getTime()
        	if (dueDate.getMonth()<11) 
                	dueDate.setMonth(dueDate.getMonth()+1)
                else {
                    dueDate.setYear(dueDate.getYear()+1)
                    dueDate.setMonth(0)
                }
            dueDate = dueDate.minus(1)
            updated = true        	
        	break
        case'Ежеквартально':
        	cal.set(Calendar.DAY_OF_MONTH, 1)
            dueDate = cal.getTime()
        	switch(today.getMonth()){
            	case 0:
                case 1:
                case 2:
                	dueDate.setMonth(0)
                	break
                case 3:
                case 4:
                case 5:
                	dueDate.setMonth(3)
                	break
                case 6:
                case 7:
                case 8:
                	dueDate.setMonth(6)
                	break
                case 9:
                case 10:
                case 11:
                	dueDate.setMonth(9)
                	break
                break
       		}
            if (dueDate.getMonth()<9)
            	dueDate.setMonth(dueDate.getMonth()+3)
            else {
            	dueDate.setYear(dueDate.getYear()+1)
                dueDate.setMonth(0)
            }
            dueDate = dueDate.minus(1)
            updated = true
        	break
        case'Раз в полгода':
        	cal.set(Calendar.DAY_OF_MONTH, 1)
            dueDate = cal.getTime()
        	switch(today.getMonth()){
            	case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                	dueDate.setMonth(0)
                	break
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                	dueDate.setMonth(6)
                	break
                break
       		}
            if (dueDate.getMonth()<6)
            	dueDate.setMonth(dueDate.getMonth()+6)
            else {
            	dueDate.setYear(dueDate.getYear()+1)
                dueDate.setMonth(0)
            }
            dueDate = dueDate.minus(1)
            updated = true
        	break
        case'Ежегодно':
        	cal.set(Calendar.DAY_OF_MONTH, 1)
            dueDate = cal.getTime()
        	dueDate.setYear(dueDate.getYear()+1)
        	dueDate.setMonth(0)
        	dueDate = dueDate.minus(1)
            updated = true
        	break
    }
    if (updated) return dueDate
    else return null
}

void createIssues(Issue issue, Collection<ApplicationUser> employees, ApplicationUser user, Date dueDate){
	def issueFactory = ComponentAccessor.getIssueFactory()
	def issueManager = ComponentAccessor.getIssueManager()
    def epiclinkCF= ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Epic Link").first()
    def categoryCF= ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Категория").first()
    def timeCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Task Frequency").first()
    def issueTypeId = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find{it.getName() == "Task"}.id
    for (ApplicationUser employee: employees){
        Issue newTask = issueFactory.getIssue()
    	newTask.setSummary(issue.summary)
		newTask.setProjectObject(issue.projectObject)
		newTask.setDescription(issue.description)
		newTask.setReporter(issue.reporter)
    	newTask.setCustomFieldValue(epiclinkCF,issue)
    	newTask.setCustomFieldValue(categoryCF,categoryCF.getValue(issue))
    	newTask.setCustomFieldValue(timeCF,timeCF.getValue(issue))
    	newTask.setIssueTypeId(issueTypeId)
        newTask.setAssignee(employee)
        newTask.setDueDate(dueDate.toTimestamp())
		Map<String,Object> newIssueParams = ["issue" : newTask] as Map<String,Object>
		issueManager.createIssueObject(user, newIssueParams)
    }
}

