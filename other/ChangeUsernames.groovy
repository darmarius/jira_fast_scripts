
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.CrowdDirectoryService;
import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.OperationType;
import com.atlassian.crowd.embedded.api.User;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.Level
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.manager.directory.DirectoryManager
import com.atlassian.crowd.model.user.UserWithAttributes;
import java.text.MessageFormat;
import com.atlassian.jira.user.ApplicationUser
import org.apache.http.HttpEntity
import org.apache.http.client.methods.*
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.apache.http.entity.StringEntity
import org.apache.http.entity.ContentType

String adminUsername = "admin"
String adminPass = "1"
ChangeUsername cu = new ChangeUsername(adminUsername,adminPass)
Collection<ApplicationUser> allUsers =  ComponentAccessor.userManager.getAllApplicationUsers().findAll{ 
  it.emailAddress.contains("andda") 
}.each{
  cu.execute(it)
}
return allUsers


public class ChangeUsername{
  private static final Logger log = Logger.getLogger(ChangeUsername.class);

  DirectoryManager directoryManager = ComponentAccessor.getComponent(com.atlassian.crowd.manager.directory.DirectoryManager)
  Long targetDirectory
  String targetDirectoryName = "Jira Internal Directory"
  String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
  String adminUsername
  String adminToken

  public ChangeUsername(String adminUsername, String adminPass) {
    this.log.setLevel(Level.DEBUG)
    Directory jiraDirectory = directoryManager.findDirectoryByName(targetDirectoryName)
    this.targetDirectory = jiraDirectory.id;
    this.adminUsername = adminUsername
    this.adminToken = Base64.getEncoder().encodeToString("${adminUsername}:${adminPass}".getBytes());
  }
  
  public String execute(ApplicationUser applicationUser) {
    String oldUsername = applicationUser.getName()
    log.debug "Start with user ${oldUsername}"
    if(oldUsername == adminUsername){
      log.error "User is admin!"
      return "SKIPPED"
    }
    if(applicationUser.getDirectoryId() != targetDirectory){
      log.error "User not from ${targetDirectoryName}"
      return "FAILED"
    }
    User user = null;
    try {
      user = this.directoryManager.findUserWithAttributesByName(targetDirectory, oldUsername);
    } catch (Exception e) {
      log.warn(MessageFormat.format("Could not retrieve user for user - {0} in directory - {1}", new Object[] { user.getName(), targetDirectoryName }), (Throwable)e);
      return "FAILED"
    } 
    String newUsername = usernameFromEmail(user as User)
    try {
      User duplicateUser = this.directoryManager.findUserByName(targetDirectory, newUsername);
      if(duplicateUser){
        log.error "User ${newUsername} already exists. Switch to use email"
        newUsername = user.getEmailAddress()
        User secondDuplicateUser = this.directoryManager.findUserByName(targetDirectory, newUsername);
        if(duplicateUser){
          log.error "Second duplicate user ${newUsername} already exists. Skipp ${user.getName()}"
          return "SKIPPED"
        }
      }
    } catch (Exception e) {} 
    HttpEntity entity = getEntityForRequest(["name":newUsername])
    CloseableHttpResponse request = sendPutRequest(oldUsername, entity)
    String responseCode = request.statusLine.statusCode
    String responseText = request.entity.content.text
    if (responseCode != "200"){
      log.error("FAILED change username for ${oldUsername} - ${responseCode} due ${responseText}")
      return
    }
    log.debug "${oldUsername} changed to ${newUsername}"
    return "SUCCESS"
  }

  HttpEntity getEntityForRequest(Map<String, String> body){
        ObjectNode jsonData = JsonNodeFactory.instance.objectNode()
        body.each{k,v -> jsonData.put(k,v)}
        return new StringEntity(jsonData.toString(), ContentType.APPLICATION_JSON)
  }

  CloseableHttpResponse sendPutRequest(String oldUsername, HttpEntity entity){
        String url = "${baseUrl}/rest/api/2/user?username=${oldUsername}"
        CloseableHttpResponse resp
        Map<String, String> headers = ["Authorization" : "Basic ${this.adminToken}".toString()]
        try{
            CloseableHttpClient httpClient = HttpClientBuilder.create().build()
            HttpPut httpPut = new HttpPut(url)
            if(headers) headers.each{key,value -> httpPut.setHeader(key, value)}
            if(entity) httpPut.setEntity(entity)
            resp = httpClient.execute(httpPut)
       	}
        catch(Exception e){
            log.error "PUT error: ${e.message}"
       	}
        finally {
            return resp
    	}
    }

  String usernameFromEmail(User user){
    String email = user.getEmailAddress()
    String usernameForUpdate = email.split("@")[0]
    return usernameForUpdate
  }
}
