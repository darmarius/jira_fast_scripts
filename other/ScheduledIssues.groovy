import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.workflow.IssueWorkflowManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.bc.issue.IssueService
import com.opensymphony.workflow.loader.ActionDescriptor
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueInputParametersImpl
import com.atlassian.jira.util.ErrorCollection
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.util.ImportUtils
import com.atlassian.jira.issue.IssueFactory
import java.time.temporal.IsoFields
import java.time.LocalDate
import org.apache.log4j.Level
import org.apache.log4j.Logger

new Class_ScheduledIssues().run()

class Class_ScheduledIssues {
    UserManager userManager = ComponentAccessor.getUserManager()
    IssueService issueService = ComponentAccessor.getIssueService()
    SearchService searchService = ComponentAccessor.getComponent(SearchService.class)
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class)
    ApplicationUser sysUser = userManager.getUserByName("jira")
    IssueFactory issueFactory = ComponentAccessor.getIssueFactory()
    IssueWorkflowManager issueWorkflowManager = ComponentAccessor.getComponent(IssueWorkflowManager.class)
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
    Logger log = Logger.getLogger(this.class)

    void run() {
        log.setLevel(Level.DEBUG)
        long scriptStartTime = System.currentTimeMillis()
        log.debug("Script start")

        HashMap<String, String> jqlMap = new HashMap<String, String>()

        // Add config here
        jqlMap.put("SD","project = ITP AND component = Timesheet_SDINT  and status = Open")
		

        Set<String> keySet = jqlMap.keySet()
        for (String key: keySet){
            rotate(jqlMap.get(key))
        }

        long scriptWorkTime = System.currentTimeMillis() - scriptStartTime
        log.debug("Script work time: ${scriptWorkTime} ms.")
    }

    ArrayList<MutableIssue> getIssuesByJql(String jqlQuery) {
        ArrayList<MutableIssue> issuesByJql = new ArrayList<MutableIssue>()
        SearchService.ParseResult parseResult = searchService.parseQuery(sysUser, jqlQuery)
        if (parseResult.isValid()) {
            def searchResult = searchService.search(sysUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
            issuesByJql = (ArrayList<MutableIssue>)searchResult.results.collect { issueManager.getIssueObject(it.id) }
            log.debug("Results (${issuesByJql.size().toString()}) (Query: ${jqlQuery})")
        } else {
            log.error("Search query not valid: " + jqlQuery)
        }

        return issuesByJql
    }

    void rotateSD(String query){
        query = "(${query}) and created >= startOfWeek()"
        ArrayList<MutableIssue> issuesForRotation = getIssuesByJql(query)
        for (MutableIssue issue: issuesForRotation){
            createNewIssue(issue)
        }
    }

    void createNewIssue(MutableIssue issue){
        ApplicationUser assignee = issue.getAssignee()

        Integer weekNumber = LocalDate.now().get (IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        String summary = "Issue ${assignee.getDisplayName()} W${weekNumber} TimeSheet" 

        MutableIssue newTask = issueFactory.getIssue()
        newTask.setIssueTypeId(issue.getIssueType().getId())
        newTask.setProjectObject(issue.getProjectObject())
        newTask.setSummary(summary)
        newTask.setReporter(sysUser)
        newTask.setAssignee(assignee)
        newTask.setLabels(issue.getLabels())
        newTask.setPriority(issue.getPriority())

        CustomField epicCF = customFieldManager.getCustomFieldObjectsByName("Epic Link").iterator().next()
        newTask.setCustomFieldValue(epicCF, issue.getCustomFieldValue(epicCF))

        MutableIssue newIssue = issueManager.createIssueObject(sysUser, newTask) as MutableIssue

        ArrayList<String> labels = new ArrayList<String>()
        labels.add("W" + weekNumber)
        labelManager.setLabels(sysUser,newIssue.id,labels.toSet(),false,false)

        reindexIssue(newIssue)

        log.debug("${newIssue.key} created ${summary}")
    }

    void reindexIssue(MutableIssue issue)     {
        boolean wasIndexing = ImportUtils.isIndexIssues()
        ImportUtils.setIndexIssues(true)
        ComponentAccessor.getComponent(IssueIndexingService).reIndex(issue)
        ImportUtils.setIndexIssues(wasIndexing)
    }
}
