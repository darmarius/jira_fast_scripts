import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.util.ImportUtils
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import com.atlassian.jira.issue.worklog.WorklogImpl
import groovy.json.JsonSlurper
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.issue.worklog.Worklog
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.text.SimpleDateFormat
import org.apache.log4j.Level
import org.apache.log4j.Logger

Class_WorklogCreator script = new Class_WorklogCreator()
script.run()

class Class_WorklogCreator {
    JsonSlurper jsonSlurper = new JsonSlurper()
    UserManager userManager = ComponentAccessor.getUserManager()
    WorklogManager worklogManager = ComponentAccessor.getWorklogManager()
    SearchService searchService = ComponentAccessor.getComponent(SearchService.class)
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    ApplicationUser sysUser = userManager.getUserByName("jira")
    HttpClient client = new HttpClient()
    String remoteHost = "https://sd.jira.com"
    Header authHeader = new Header("Authorization", "Basic ") // Change password
    Logger log = Logger.getLogger("WorkLogger")

    ArrayList<List<String>> config = new ArrayList<List<String>>()

    void run() {
        log.setLevel(Level.DEBUG)
        long scriptStartTime = System.currentTimeMillis()
        log.debug("Script start")

        //JQL mapping - key = local jira, value = remote jira 
        config.addAll(["component = SD_Worklog","project in (SD)"])

        for (List<String> configItem: config) {
            String localJql = configItem[0]
            String remoteJql = configItem[1]
            for (MutableIssue localIssue : getIssuesByJql(localJql)) fillWorklogs(localIssue, remoteJql)
        }

        long scriptWorkTime = System.currentTimeMillis() - scriptStartTime
        log.debug("Script work time: ${scriptWorkTime} ms.")
    }

    Object[] getRemoteIssuesByJQL(String jqlQuery) {
        log.debug(jqlQuery)
        Object[] issues
        String method = "/rest/api/2/search?jql="
        String URL = remoteHost + method + URLEncoder.encode(jqlQuery, "UTF-8")
        log.debug("GET ${URL}")

        GetMethod getJqlResults = new GetMethod(URL)

        getJqlResults.addRequestHeader(new Header("X-Atlassian-Token", "no-check"))
        getJqlResults.addRequestHeader(new Header("Cache-Control", "no-cache"))
        getJqlResults.addRequestHeader(authHeader)

        int responseStatus = client.executeMethod(getJqlResults)
        if (responseStatus == 200) {
            log.debug("200 OK")
            InputStream responseQueueBody = getJqlResults?.responseBodyAsStream

            if (responseQueueBody != null) {
                def responseJSON = jsonSlurper.parse(responseQueueBody)
                issues = responseJSON.getAt("issues") as Object[]
            }
        } else {
            log.error("Response status = ${responseStatus}")
        }
        return issues
    }

    List<MutableIssue> getIssuesByJql(String jqlQuery) {
        List<MutableIssue> issuesByJql = new ArrayList<MutableIssue>()
        SearchService.ParseResult parseResult = searchService.parseQuery(sysUser, jqlQuery)
        if (parseResult.isValid()) {
            def searchResult = searchService.search(sysUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
            issuesByJql = searchResult.results.collect { issueManager.getIssueObject(it.id) }
            log.debug("Results (${issuesByJql.size().toString()}) (Query: ${jqlQuery})")
        } else {
            log.error("Search query not valid: " + jqlQuery)
        }

        return issuesByJql
    }

    ArrayList<Object> getWorklogsForIssueId(String issueId){
        ArrayList<Object> worklogs = new ArrayList<Object>()
        String method = "/rest/api/2/issue/${issueId}/worklog"
        String URL = remoteHost + method
        log.debug("GET ${URL}")

        GetMethod getWorklogs = new GetMethod(URL)

        getWorklogs.addRequestHeader(new Header("X-Atlassian-Token", "no-check"))
        getWorklogs.addRequestHeader(new Header("Cache-Control", "no-cache"))
        getWorklogs.addRequestHeader(authHeader)

        int responseStatus = client.executeMethod(getWorklogs)
        if (responseStatus == 200) {
            log.debug("200 OK")
            InputStream responseQueueBody = getWorklogs?.responseBodyAsStream
            if (responseQueueBody != null) {
                def responseJSON = jsonSlurper.parse(responseQueueBody)
                worklogs = responseJSON.getAt("worklogs") as ArrayList<Object> 
            }
        } else {
            log.error("Response status = ${responseStatus}")
        }
        return worklogs
    }

    void fillWorklogs (MutableIssue localIssue, String remoteJql){
        ApplicationUser assignee = localIssue.getAssignee()

        if (assignee != null){
            log.debug("${localIssue.key} Assignee: ${assignee.getUsername()}")
            HashMap<Long,Long> worklogsMapping = getWorkLogsMapping(assignee)
            log.debug("${localIssue.key} ${worklogsMapping}")
            String jqlForLoggedWork = "${remoteJql} and worklogDate >= startOfYear() and worklogAuthor = ${assignee.getUsername()}"
            Object[] remoteIssues = getRemoteIssuesByJQL(jqlForLoggedWork)
            if(remoteIssues?.size() > 0){
                for (Object remoteIssue: remoteIssues){
                    ArrayList<Object> remoteWorklogs = getWorklogsForIssueId(remoteIssue.getAt("id").toString())
                    log.debug("${localIssue.key} ${remoteWorklogs}")
                    for (def remoteWorklog: remoteWorklogs){
                        Long remoteWorklogId
                        try {
                            remoteWorklogId = Long.parseLong(remoteWorklog.getAt("id").toString())
                        }
                        catch (NumberFormatException exception) {
                            log.debug("${localIssue.key} wrong ID value: ${remoteWorklog.getAt("id")}")
                        }
                        if (remoteWorklogId != null){
                            Long localWorkLogId = worklogsMapping.get(remoteWorklogId)
                            log.debug("${localIssue.key} Remote ${remoteWorklogId} Local ${localWorkLogId}")
                            if (localWorkLogId == null){
                                log.debug("${localIssue.key} Creating worklog")

                                String remoteWorkLogCreatorKey = remoteWorklog?.getAt("author")?.getAt("name")

                                //Date
                                String started = remoteWorklog.getAt("started").toString()
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                                Date day = null
                                day = format.parse(started)
                                log.debug(day)

                                if (remoteWorkLogCreatorKey != null && remoteWorkLogCreatorKey == assignee.getUsername()){
                                    log.debug("${remoteWorklog.getAt("id")} Target worklog")

                                    //Description
                                    String workLogDescription = "(${remoteIssue.getAt("key")}) ${remoteIssue.getAt("fields").getAt("summary")} [${remoteWorklog.getAt("id")}]"

                                    //Time spent
                                    String spentSecondsString = remoteWorklog.getAt("timeSpentSeconds").toString()
                                    int spentSeconds
                                    try{
                                        spentSeconds = Integer.parseInt(spentSecondsString)
                                    }
                                    catch (NumberFormatException exception){
                                        log.debug("Format error")
                                    }



                                    if (workLogDescription && spentSeconds && day){
                                        Long timeSpent = localIssue.getTimeSpent()                 //seconds
                                        if (timeSpent == null){
                                            timeSpent = 0
                                        }
                                        log.debug("${localIssue.key}  ${assignee.key} Logging ${spentSeconds} for ${day}")
                                        WorklogImpl worklog = new WorklogImpl(worklogManager, localIssue, null, assignee.getUsername().toLowerCase(), workLogDescription, day, null, null, spentSeconds)
                                        worklogManager.create(sysUser, worklog, null, false)
                                        timeSpent += spentSeconds
                                        localIssue.setTimeSpent(timeSpent)

                                        Long estimate = 0
                                        Long originaEstimate = localIssue.getOriginalEstimate()
                                        if (originaEstimate == null){
                                            originaEstimate = 0
                                        }

                                        if ((originaEstimate - timeSpent) > 0){
                                            estimate = originaEstimate - timeSpent
                                        }

                                        localIssue.setEstimate(estimate)
                                        issueManager.updateIssue(sysUser, localIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
                                        reindexIssue(localIssue)
                                    }
                                    else{
                                        log.debug("workLogDescription = ${workLogDescription}")
                                        log.debug("spentSeconds = ${spentSeconds}")
                                        log.debug("day = ${day}")
                                    }

                                }
                                else{
                                    log.debug("${remoteWorklog.getAt("id")} Not target worklog")
                                }
                            }
                            else{
                                log.debug("${localIssue.key} Worklog already exists")
                            }
                        }
                    }
                }
            }
        }
    }
	
    
    
    HashMap<Long,Long> getWorkLogsMapping(ApplicationUser assignee){
        Map<Long,Long> worklogMap = new HashMap <Long,Long>()
        List<MutableIssue> issuesForAssignee = getIssuesByJql("""component = SD_Worklog and assignee = ${assignee.username}""")
        for(MutableIssue localIssue: issuesForAssignee){
            List<Worklog> existingWorklogs = worklogManager.getByIssue(localIssue)
            for (Worklog existingWorklog : existingWorklogs) {
                //log.debug(existingWorklog.getComment())
                Pattern pattern = Pattern.compile("[\\[](.*?)[\\]]")
                Matcher matcher = pattern.matcher(existingWorklog?.getComment())
                List<String> idList = new ArrayList<>()
                while (matcher.find()) {
                    idList.add(matcher.group(1))
                }
                if (idList.size() > 0){
                    String sdWorklogId = idList.last()
                    Long sdWorklogIdLong
                    if (sdWorklogId != "" && sdWorklogId != null){
                        try {
                            sdWorklogIdLong = Long.parseLong(sdWorklogId)
                        }
                        catch (NumberFormatException exception) {
                            log.debug("Wrong value: ${sdWorklogId}")
                        }
                        if (sdWorklogIdLong != null){
                            worklogMap.put(sdWorklogIdLong, existingWorklog.getId())
                        }
                    }
                }
            }
        }
        return worklogMap
    }

    void reindexIssue(MutableIssue issue)     {
        boolean wasIndexing = ImportUtils.isIndexIssues()
        ImportUtils.setIndexIssues(true)
        ComponentAccessor.getComponent(IssueIndexingService).reIndex(issue)
        ImportUtils.setIndexIssues(wasIndexing)
    }

}