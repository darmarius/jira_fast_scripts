package helper

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.util.json.JSONObject
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.workflow.TransitionOptions
import com.atlassian.mail.Email
import com.atlassian.mail.queue.SingleMailQueueItem
import com.atlassian.query.Query
import org.apache.http.HttpEntity
import org.apache.http.client.methods.*
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.apache.http.entity.StringEntity
import org.apache.http.entity.ContentType

class BasicHelper{

    static Logger log = Logger.getLogger("BasicHelper")
    static ApplicationUser sysUser = ComponentAccessor.userManager.getUserByName("jiraadmin")
    static IssueManager issueManager = ComponentAccessor.getIssueManager()
    static EventDispatchOption DO_NOT_DISPATCH = EventDispatchOption.DO_NOT_DISPATCH
    static EventDispatchOption ISSUE_UPDATED = EventDispatchOption.ISSUE_UPDATED
    static EventDispatchOption ISSUE_DELETED = EventDispatchOption.ISSUE_DELETED
    static EventDispatchOption ISSUE_ASSIGNED = EventDispatchOption.ISSUE_ASSIGNED


    static Option getOptionByName(CustomField cf, String optionName){
        log.setLevel(Level.DEBUG)
        List<FieldConfig> fieldConfigs = cf.configurationSchemes*.configs*.values().flatten() as List<FieldConfig>
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager()
        for(FieldConfig fieldConfig : fieldConfigs){
            Options options = optionsManager.getOptions(fieldConfig)
            log.debug "Options in the fieldConfig ${fieldConfig.getName()} are ${options}"
            Option resultOption = options.find{it -> it.getValue() == optionName}
            if(resultOption) return resultOption
            else log.debug "Option with name $optionName wasn't found in the fieldConfig ${fieldConfig.getName()}."
        } 
    }

    static CloseableHttpResponse sendGetRequest(String url, Map<String, String> headers){
        log.setLevel(Level.DEBUG)
        CloseableHttpResponse resp
        try{
            CloseableHttpClient httpClient = HttpClientBuilder.create().build()
            HttpGet httpGet = new HttpGet(url)
            if(headers) headers.each{key,value -> httpGet.setHeader(key, value)}
            resp = httpClient.execute(httpGet)
       	}
        catch(Exception e){
            log.error "GET error: ${e.message}"
       	}
        finally {
            log.debug "GET request to: ${url} finished"
            return resp
    	}
    }

    static CloseableHttpResponse sendPostRequest(String url, HttpEntity entity, Map<String, String> headers){//для получения entity из строки метод - getEntityForRequest
        log.setLevel(Level.DEBUG)
        CloseableHttpResponse resp
        try{
            CloseableHttpClient httpClient = HttpClientBuilder.create().build()
            HttpPost httpPost = new HttpPost(url)
             if(headers) headers.each{key,value -> httpPost.setHeader(key, value)}
            httpPost.setEntity(entity)
            resp = httpClient.execute(httpPost)
		}
        catch(Exception e){
            log.error "POST error: ${e.message}"
        }
        finally{
            log.debug "POST request to: ${url} done"
            return resp
        }
    }

    static HttpEntity getEntityForRequest(Map<String, String> body){
        ObjectNode jsonData = JsonNodeFactory.instance.objectNode()
        body.each{k,v -> jsonData.put(k,v)}
        return new StringEntity(jsonData.toString(), ContentType.APPLICATION_JSON)
    }

    static CloseableHttpResponse sendPutRequest(String url, HttpEntity entity, Map<String, String> headers){
        log.setLevel(Level.DEBUG)
        CloseableHttpResponse resp
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
            log.debug "PUT request to: ${url} finished"
            return resp
    	}
    }

    static CloseableHttpResponse sendDeleteRequest(String url, Map<String, String> headers){
        log.setLevel(Level.DEBUG)
        CloseableHttpResponse resp
        try{
            CloseableHttpClient httpClient = HttpClientBuilder.create().build()
            HttpDelete httpDelete = new HttpDelete(url)
            if(headers) headers.each{key,value -> httpDelete.setHeader(key, value)}
            resp = httpClient.execute(httpDelete)
       	}
        catch(Exception e){
            log.error "GET error: ${e.message}"
       	}
        finally {
            log.debug "GET request to: ${url} finished"
            return resp
    	}
    }

    static ApplicationUser getUserByDisplayName(String displayName){
        try{
            return ComponentAccessor.userSearchService.findUsersByFullName(displayName)?.first()
        }
        catch(Exception e){
            log.error "User with display name $displayName is not been."
            return null
        }
        
    }

    static void sendMail(String emailAddress, String subject, String body) {
        try{
            Email mail = new Email(emailAddress)
            mail.setSubject(subject)
            mail.setBody(body)
            mail.setMimeType("text/html")
            SingleMailQueueItem item = new SingleMailQueueItem(mail)
            ComponentAccessor.mailQueue.addItem(item)
            log.error "Mail '$subject' was sent to $emailAddress"
        }
        catch(Exception e){
            log.error "Mail wasn't sent. Exception - ${e.message}"
        }
    }

    static void createComment(MutableIssue issue, ApplicationUser author, String commentText, boolean internal){
        try{
            final SD_PUBLIC_COMMENT = "sd.public.comment"
            def properties = [(SD_PUBLIC_COMMENT): new JSONObject().put("internal", internal)]
            ComponentAccessor.getCommentManager().create(issue, author, commentText, null, null, new Date(), properties, true)
            log.debug "Comment was created for issue $issue."
            }
        catch(Exception e){
            log.error "Comment wasn't created. Exception - ${e.message}"
        }
    }

    static void updateIssue(MutableIssue issue, EventDispatchOption dispatchOption, boolean sendEmail) {
        issueManager.updateIssue(sysUser,issue,dispatchOption,sendEmail)
    	ComponentAccessor.getComponent(IssueIndexingService.class).reIndex(issue)
    }

    static boolean transitIssue(MutableIssue issue, String transitionName){
        def workflow = ComponentAccessor.workflowManager.getWorkflow(issue)
        IssueService issueService = ComponentAccessor.getIssueService()
        for(int transitionID : workflow.getActionsByName(transitionName)*.id){
            def builder =  new TransitionOptions.Builder()
            def transopt = builder.skipConditions().skipValidators().skipPermissions()
            IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(sysUser, issue.getId(), transitionID, issueService.newIssueInputParameters(),transopt.build())
            if (transitionValidationResult.isValid()) {
                IssueService.IssueResult transitionResult = issueService.transition(sysUser, transitionValidationResult)
                log.debug "Issue $issue was transited by transition '$transitionName' successfully"
                return true
            }
            else log.debug "ErrorCollection for transitionValidationResult: ${transitionValidationResult.getErrorCollection()}, WarningCollection: ${transitionValidationResult.getWarningCollection()}"
        }
        return false
    }

    static List<MutableIssue> getIssuesByJql(String jql){
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
        SearchService searchService = ComponentAccessor.getComponent(SearchService)
        List<MutableIssue> resultList = new ArrayList<MutableIssue>()
        Query query = jqlQueryParser.parseQuery(jql)
        SearchResults searchResults = searchService.search(sysUser, query, PagerFilter.getUnlimitedFilter())
        try{
            List<MutableIssue> jqlResult = searchResults.results.collect {issueManager.getIssueObject(it.id)} as List<MutableIssue>
            resultList.addAll(jqlResult)
        }
        catch(Exception e){
            log.debug "JQL '$jql' generated an Exception ${e.message}"
        }
        return resultList
    }
}