import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.applinks.JiraApplicationLinkService

moveIssueToOpen("TASK-1","111")

void moveIssueToOpen(String key, String transitionId){
	String inputData = """{   
		"transition": {
			"id": "${transitionId}" 
		}
	}"""
	def jiraAppId = ComponentAccessor.getComponent(JiraApplicationLinkService.class).getApplicationLinks().find{it.name=="Remote Jira"}
	def baseUrl = jiraAppId.getRpcUrl()
	String remoteLink = "${baseUrl}/rest/api/latest/issue/${key}/transitions"
	log.debug("post ${remoteLink}")
	log.debug(inputData)
	URL url = new URL(remoteLink)
  	HttpURLConnection post = (HttpURLConnection) url.openConnection()
	post.setRequestProperty("Content-Type", "application/json")
	post.setRequestProperty("Authorization", "Basic ")
	String response
	post.with {
        doOutput = true
        requestMethod = 'POST'
        outputStream.withWriter { writer ->
            writer << inputData
        }
        response = content.text
	}	
	log.debug(post.getResponseCode()+ " transit issue response code")
}