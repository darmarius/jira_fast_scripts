import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonSlurper
import com.atlassian.jira.applinks.JiraApplicationLinkService
import java.net.HttpURLConnection

createRemoteIssue("summary","desc","EPIC-1","reporter","assignee","Task")
       
String createRemoteIssue(String summary, String description, String epicLink, String reporter, String assignee, String issueType){
    
	String inputData = """{
		"fields": {
			"project":{"key": "PROJECT"},
			"summary": "${summary}",
			"description": "${description}",
			"issuetype": {"name": "${issueType}"},
			"customfield_10201": "${epicLink}"}
		}   
	}"""
	def jiraAppId = ComponentAccessor.getComponent(JiraApplicationLinkService.class).getApplicationLinks().find{it.name=="Remote Jira"}
	def baseUrl = jiraAppId.getRpcUrl()
	String remoteLink = "${baseUrl}/rest/api/2/issue"
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
	def jsonSlurper = new JsonSlurper()
	def content = jsonSlurper.parseText(response)	
	setUsersToIssue(content.getAt("key"),assignee,reporter)
	return content.getAt("key")
}

void setUsersToIssue(String key, String assignee, String reporter){
    def assigneeObject = getRemoteUser(assignee,log)
	def reporterObject = getRemoteUser(reporter,log)
	String inputData = """{
		"fields": {
			"assignee": ${assigneeObject},
			"reporter": ${reporterObject}
		}   
	}"""
    
	def jiraAppId = ComponentAccessor.getComponent(JiraApplicationLinkService.class).getApplicationLinks().find{it.name=="Remote Jira"}
	def baseUrl = jiraAppId.getRpcUrl()
	String remoteLink = "${baseUrl}/rest/api/latest/issue/${key}"
	log.debug("put ${remoteLink}")
    log.debug(inputData)
	URL url = new URL(remoteLink)
  	HttpURLConnection put = (HttpURLConnection) url.openConnection()
	put.setRequestProperty("Content-Type", "application/json")
	put.setRequestProperty("Authorization", "Basic ")
	String response
	put.with {
        doOutput = true
        requestMethod = 'PUT'
        outputStream.withWriter { writer ->
            writer << inputData
        }
	}
    log.debug(put.getResponseCode()+" adding users response code")
}

String getRemoteUser(String username){    
	def jiraAppId = ComponentAccessor.getComponent(JiraApplicationLinkService.class).getApplicationLinks().find{it.name=="Remote Jira"}
	def baseUrl = jiraAppId.getRpcUrl()
	String remoteLink = "${baseUrl}/rest/api/2/user?username=${username}"
	log.debug("get ${remoteLink}")
	URL url = new URL(remoteLink)
  	HttpURLConnection get = (HttpURLConnection) url.openConnection()
	get.setRequestProperty("Content-Type", "application/json")
	get.setRequestProperty("Authorization", "Basic ")
	String response
	get.with {
        requestMethod = 'GET'
        response = content.text
	}	
	return response.toString()
}