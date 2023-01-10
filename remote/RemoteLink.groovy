import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.applinks.JiraApplicationLinkService
import java.net.HttpURLConnection

String createRemoteLink(String currentIssue, String remoteIssue, String remoteLinkName){
    MutableIssue issue = ComponentAccessor.issueManager.getIssueObject(currentIssue)
    log.info("linking ${issue.key} with ${remoteIssue}")
    def jiraAppId = ComponentAccessor.getComponent(JiraApplicationLinkService.class).getApplicationLinks().find{it.name==remoteLinkName}.getId()
    String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
    String remoteLink = "${baseUrl}/secure/LinkJiraIssue.jspa?"
    remoteLink += "id=${issue.id}"
    remoteLink += "&jiraAppId=${jiraAppId}"
    remoteLink += "&linkDesc=relates%20to"
    remoteLink += "&issueKeys=${remoteIssue}"
    remoteLink += "&createReciprocal=true"
    log.info("remoteLink ${remoteLink}")
    URL url = new URL(remoteLink)
    HttpURLConnection post = (HttpURLConnection) url.openConnection()
    post.setRequestMethod("POST")
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty("Authorization", "Basic ") // user should have access to both projects
    post.setRequestProperty("X-Atlassian-Token", "no-check")
    def postRC = post.getResponseCode()
}