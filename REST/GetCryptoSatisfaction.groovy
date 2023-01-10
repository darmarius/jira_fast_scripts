import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import com.atlassian.jira.component.ComponentAccessor
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import groovy.transform.BaseScript
import javax.ws.rs.core.MultivaluedMap
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.util.ImportUtils
import com.atlassian.jira.workflow.IssueWorkflowManager
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.context.IssueContext
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.context.IssueContextImpl
import com.atlassian.jira.config.util.JiraHome
import org.apache.log4j.Level
import org.apache.log4j.Logger
import com.atlassian.jira.bc.issue.search.SearchService
import javax.ws.rs.core.Response
import javax.xml.bind.DatatypeConverter
import java.security.MessageDigest
import java.text.SimpleDateFormat
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey

@BaseScript CustomEndpointDelegate delegate

sendSatisfaction(httpMethod: "GET", groups: ["satisfactionGroup"]) {
    MultivaluedMap queryParams, String body ->
        String errorPage =  """<p align="center"><br>Oops! Something went wrong<br></p>"""
        log.setLevel(Level.DEBUG)
        if (queryParams?.size() > 0){
            List<String> tokenList = queryParams.get("token")
            if (tokenList){
                String token = tokenList.first().replace("[","").replace("]","").replaceAll(" ","+")
                List<String> flagList = queryParams.get("flag")
                if (flagList){
                	def flag = flagList.first().replace("[","").replace("]","")
                    if (flag) {
                        String commentBody = ""
                        //List<String> markList = queryParams.get("mark")
                        List<String> commentBodyList = queryParams.get("content")
                        //if (markList) mark = markList.first().replace("[","").replace("]","")
                        if (commentBodyList) commentBody = commentBodyList.first().replace("[","").replace("]","")
                        Class_SetSatisfaction script = new Class_SetSatisfaction()
                        log.debug ("Flag is ${flag}")
                        log.debug ("Comment is ${commentBody}")
                        script.run(token,flag,commentBody)
                    }
                    else {
                    	log.debug ("Flag is invalid")
                		Response.status(404).type(MediaType.TEXT_HTML).entity(errorPage.toString()).build()
                    }
                }
                else{
                    log.debug ("Flag is empty")
                	Response.status(404).type(MediaType.TEXT_HTML).entity(errorPage.toString()).build()
                }
            }
            else{
                log.debug ("Token is empty")
                Response.status(404).type(MediaType.TEXT_HTML).entity(errorPage.toString()).build()
            }
        }
        else{
            log.debug ("No query params")
            Response.status(404).type(MediaType.TEXT_HTML).entity(errorPage.toString()).build()
        }
}

class Class_SetSatisfaction {
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
    UserManager userManager = ComponentAccessor.getUserManager()
    SearchService searchService = ComponentAccessor.getComponent(SearchService.class)
    IssueManager issueManager = ComponentAccessor.getIssueManager()
    IssueWorkflowManager issueWorkflowManager = ComponentAccessor.getComponent(IssueWorkflowManager.class)
    IssueService issueService = ComponentAccessor.issueService
    OptionsManager optionsManager = ComponentAccessor.getOptionsManager()

    String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
    ApplicationUser sysUser = userManager.getUserByName("jira_service")
    String jiraHome = ComponentAccessor.getComponent(JiraHome).getHome()
    String templateDirectory = "${jiraHome}/HTML"
    String satisfactionCfName = "Customer satisfaction"
    private static String password = "1234"
    private static String salt = "4321"
    private static String vector = "2233"
    String defaultError = """<p align="center"><br>Oops! Some error occurred.<br></p><p>Please contact your system administrator</p>"""
	
    Logger log = Logger.getLogger(this.class)
    
    Response run(String token, String flag, String commentBody) {
        
		ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.userManager.getUserByName('JIRA_Service'))
        Response response = Response.status(403).type(MediaType.TEXT_HTML).entity(defaultError).build()
        log.setLevel(Level.DEBUG)
        String decodedToken = dencrypt(token)
        if(decodedToken != null){
            log.debug("Token decoded successfully ${decodedToken}")
            String[] params = decodedToken.split(":")
            String issueIdString = params[0]
            String userIdString = params[1]
            String ratingString = params[2]
			String hash = params[3]
			Date dateCreated = new SimpleDateFormat("dd/MM/yyyy").parse(params[4])
			
            MutableIssue issue = getIssueByStringId (issueIdString)
            if (issue != null){
                log.debug("Issue found: ${issue.key}")
                if(checkHash(issue, hash)){
                    log.debug("Hash for ${issue.key} is correct")
                    ApplicationUser user = getUserByStringId (userIdString)
                    if (user != null){
                        log.debug("User ${user.name} found and active")
                        if (flag=="comment" && setComment(issue, user, commentBody)){
                        	log.debug("Successfully commented for ${issue.key}")
                            return successMessage(issue)
                        }
                        if (flag =="check"){
                        	log.error("Send answer to service")
							if (dateCreated > new Date().minus(7)){
                            	log.debug("sending response with mark ${ratingString}")
                                String okPage = """ {"mark":"${ratingString}"}  """
								response =  Response.status(200).type(MediaType.APPLICATION_JSON).entity(okPage).build()
								return response
							}
							else {
                            	log.debug("expired token")
								String badPage = getErrorPage("Token expired ${issue.key}", "Token expired")
								response =  Response.status(400).type(MediaType.TEXT_HTML).entity(badPage).build()
								return response
							}
                        }
                        if (!isSatisfactionExists(issue)){
                            log.debug("Satisfaction for ${issue} is NOT set yet")                            
                            if (flag=="set" && setSatisfaction(issue, user, ratingString)){
                                log.debug("Successfully set satisfaction for ${issue.key}")
                                return successMessage(issue)
                            }
                            else{
                                log.error("Cannot set satisfaction for ${issue.key}")
                                return unknownError()
                            }                            
                        }
                        else{
                            log.error("Satisfaction for ${issue.key} is already set")
                            String errorPage = getErrorPage("Oops! ${issue.key} is already rated.", "You can't change your decision")
                            response =  Response.status(400).type(MediaType.TEXT_HTML).entity(errorPage).build()
                            return response
                        }
                    }
                    else{
                        log.error("Hash for ${issue.key} is incorrect")
                        return unknownError()
                    }
                }
                else{
                    log.error("Hash for ${issue.key} is incorrect")
                    return unknownError()
                }
            }
            else{
                log.error("Issue cannot be found")
                return unknownError()
            }
        }
        else{
            log.error("Token ${token} cannot be decoded")
            return unknownError()
        }

        return response
    }

    String decodeToken(String token) {
        try{
            byte[] decodedBytes = Base64.getDecoder().decode(token)
            String tokenDecoded = new String(decodedBytes)
            log.debug(tokenDecoded)
            return tokenDecoded
        }
        catch (Exception e){
            log.debug("Error decoding token: ${e}")
            return null
        }
    }

    String getErrorPage(String errorTitle, String errorMessage){
        String page = defaultError
        String errorTemplate = "${templateDirectory}/Satisfaction Error.html"
        File templateFile = new File(errorTemplate)
        if(templateFile.exists()){
            String fileContent = templateFile.text
            page = fileContent
                .replace('${errorTitle}', errorTitle)
                .replace('${errorMessage}',errorMessage)
        }
        else{
            log.debug("File ${errorTemplate} does not exist or you do not have permission to view it")
        }
        return page
    }

    MutableIssue getIssueByStringId (String issueIdString){
        MutableIssue issue
        Long issueId
        try{
            issueId = Long.parseLong(issueIdString)
        }
        catch (Exception e){
            log.debug(e)
        }
        if (issueId != null){
            issue = issueManager.getIssueObject(issueId)
        }
        return issue
    }

    ApplicationUser getUserByStringId (String userIdString) {
        ApplicationUser user
        Long userId
        try{
            userId = Long.parseLong(userIdString)
        }
        catch (Exception e){
            log.debug(e)
        }
        if(userId != null){
            user = userManager.getUserById(userId)?.get()
        }
        return user
    }

    boolean checkHash(MutableIssue issue, String hash){
        String issueInfo = issue.key + issue.id + password
        MessageDigest md = MessageDigest.getInstance("MD5")
        md.update(issueInfo.getBytes())
        byte[] digest = md.digest()
        String myHash = DatatypeConverter.printHexBinary(digest).toUpperCase()
        return myHash.equals(hash)
    }

    Response unknownError(){
        String errorPage = getErrorPage("Oops! Some error occurred", "Please contact your system administrator")
        return Response.status(403).type(MediaType.TEXT_HTML).entity(errorPage).build()
    }

    Response successMessage(MutableIssue issue){
        String page = defaultError
        String successTemplate = "${templateDirectory}/Satisfaction OK.html"
        File templateFile = new File(successTemplate)
        if(templateFile.exists()){
            String fileContent = templateFile.text
            page = fileContent
                .replace('${issueKey}', issue.key)
                .replace('${baseURL}',baseUrl)
        }
        else{
            log.debug("File ${successTemplate} does not exist or you do not have permission to view it")
        }

        return Response.status(200).type(MediaType.TEXT_HTML).entity(page).build()
    }

    boolean isSatisfactionExists(MutableIssue issue){
        CustomField satisfactionCF = customFieldManager.getCustomFieldObjectsByName(satisfactionCfName).iterator().next()
        if (issue?.getCustomFieldValue(satisfactionCF) != null){
            return true
        }
        else{
            return false
        }
    }

    boolean setSatisfaction(MutableIssue issue, ApplicationUser user, String rating){
        boolean success = false           
        String cfOptionValueString
        switch (rating){
            case "5": cfOptionValueString = "5 - Very good"
                break
            case "4": cfOptionValueString = "4 - Good"
                break
            case "3": cfOptionValueString = "3 - Neither good nor poor"
                break
            case "2": cfOptionValueString = "2 - Poor"
                break
            case "1": cfOptionValueString = "1 - Very poor"
                break
        }
        if (cfOptionValueString != null){
            CustomField satisfactionCF = customFieldManager.getCustomFieldObjectsByName(satisfactionCfName).iterator().next()
            IssueContext issueContext = new IssueContextImpl(issue.getProjectId(), issue.getIssueTypeId())
            FieldConfig fieldConfig = satisfactionCF.getRelevantConfig(issueContext)
            Options options = optionsManager.getOptions(fieldConfig)
            for (Option option : options.rootOptions){
                if (option.toString() == cfOptionValueString) {
                    log.debug(option)
                    issue.setCustomFieldValue(satisfactionCF, option)
                    issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
                    reindexIssue(issue)
                    break
                }
            }
            success = true
        }
        log.debug("success is ${success}")
        return success
    }
    
    boolean setComment(MutableIssue issue, ApplicationUser user, String commentBody){
        boolean success = false          
        if(commentBody) {
          		ComponentAccessor.getCommentManager().create(issue,user,commentBody,true)
            	log.debug("Comment sent")
        }
        success = true
        log.debug("success is ${success}")
        return success
    }

    void reindexIssue(MutableIssue someIssue) {
        boolean wasIndexing = ImportUtils.isIndexIssues()
        ImportUtils.setIndexIssues(true)
        Issue freshIssue = issueManager.getIssueObject(someIssue.key)
        ComponentAccessor.getComponent(IssueIndexingService.class).reIndex(freshIssue)
        ImportUtils.setIndexIssues(wasIndexing)
    }
    
    String dencrypt(String encrypted){
        IvParameterSpec ivspec = new IvParameterSpec(vector.bytes)
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128)
		SecretKey tmp = factory.generateSecret(spec)
		SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(),"")
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec)
		String decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)))
		return decrypted
    }
}
