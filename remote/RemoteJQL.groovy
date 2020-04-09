import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.applinks.JiraApplicationLinkService

String customFields = "customfield_10204, customfield_10205, summary, description"
getRemoteIssues("project = test",customFields)

String getRemoteIssues(String jql, List<String> customFields){
	def jiraAppId = ComponentAccessor.getComponent(JiraApplicationLinkService.class).getApplicationLinks().find{it.name=="Remote Jira"}
	def baseUrl = jiraAppId.getRpcUrl()
    String link = "${baseUrl}/rest/api/2/search?jql=${URLEncoder.encode(jql)}"
    if (customFields)
        link += "&fields=${customFields}"
	URL url = new URL(link)
	HttpURLConnection get = (HttpURLConnection) url.openConnection()
	get.setRequestMethod("GET")
	get.setDoOutput(true)
	get.setRequestProperty("Content-Type", "application/json")
	get.setRequestProperty("Authorization", "Basic ")
	get.setRequestProperty("X-Atlassian-Token", "no-check")
	def getRC = get.getResponseCode()
	def br = new BufferedReader(new InputStreamReader(get.getInputStream()))
	String strCurrentLine;
	String response;
	while ((strCurrentLine = br.readLine()) != null) {
		response += strCurrentLine
	}
	def jsonSlurper = new JsonSlurper()
	def content = jsonSlurper.parseText(response.replace("null",""))
	Collection issues =  (Collection)content.getAt("issues")
	String cfValues
	for (def issue : issues){
		cfValues += "<option value=\"${issue.getAt("key")}\">${issue.getAt("fields").getAt("customfield_10204")}</option>"
	}
    return cfValues
}

