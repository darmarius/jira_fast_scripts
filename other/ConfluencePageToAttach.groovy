import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import com.atlassian.jira.issue.Issue
import java.io.InputStream
import org.apache.log4j.Level
import org.apache.log4j.Logger
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean
import java.io.FileOutputStream
import org.apache.commons.io.IOUtils

class Class_GetConfluencePageToAttach {
    
   	ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
   	String confluenceAuth = "Basic "
   	Logger log = Logger.getLogger(this.class)
   	String confluencePDFURL = "https://confluence.com/spaces/flyingpdf/pdfpageexport.action?pageId="
	
    String getPageID(String requestURL){
        HttpClient client = new HttpClient()
        GetMethod getPageInfo = new GetMethod(requestURL)
        getPageInfo.addRequestHeader(new Header("Authorization", confluenceAuth))
        int responseStatus = client.executeMethod(getPageInfo)
        log.debug "do get to ${requestURL}"
        if (responseStatus == 200){
            InputStream inputStream = getPageInfo.getResponseBodyAsStream()
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String pageContent = writer.toString();
            inputStream.close()
            if(pageContent.find("ajs-page-id")){
                int start = pageContent.indexOf('<meta name="ajs-page-id" content="')
                pageContent = pageContent.substring(start)
                int end = pageContent.indexOf('">')
                String pageId = pageContent.substring(34,end)
                log.debug "Page ID is ${pageId}"
                return pageId
            }
            log.debug "Page ID not found" 
            return null
        }
        log.error("FAIL - ${responseStatus} GET ${requestURL}")
        return null
    }
    
    File getPdfFromPage(String pageId){
        HttpClient client = new HttpClient()
        GetMethod getPageInfo = new GetMethod(confluencePDFURL+pageId)
        getPageInfo.addRequestHeader(new Header("Authorization", confluenceAuth))
        getPageInfo.addRequestHeader(new Header("Accept", "application/pdf"))
        int responseStatus = client.executeMethod(getPageInfo)
        log.debug "do get to ${confluencePDFURL+pageId}"
        if (responseStatus == 200){
            String fileName = " "
            def headers = getPageInfo.getResponseHeaders()
            if(headers.find{it.toString().contains("Content-Disposition:")}){
                String filenameHeader = headers.find{it.toString().contains("Content-Disposition:")}.getValue()
                int start = filenameHeader.indexOf('"')+1
                int end = filenameHeader.lastIndexOf('"')
                fileName = filenameHeader.substring(start,end).trim()
                log.debug "fileName is ${fileName}"
                
                FileOutputStream fos1 = new FileOutputStream("/tmp/${fileName}")
                InputStream is1 = getPageInfo.getResponseBodyAsStream()
                byte[] ba1 = new byte[1024];
                int baLength
                while ((baLength = is1.read(ba1)) != -1) {
                    fos1.write(ba1, 0, baLength);
                }
                fos1.flush()
                fos1.close()
                is1.close()
                log.debug "/tmp/${fileName} saved"
        		return new File("/tmp/${fileName}")
            }
        }
        log.error("FAIL - ${responseStatus} GET ${confluencePDFURL+pageId}")
        return null
    }
    
    void addFileToIssue(File file, Issue issue){
    	def attachmentManager = ComponentAccessor.getAttachmentManager()
        def bean = new CreateAttachmentParamsBean.Builder()
         .file(file)
         .filename(file.name)
         .contentType("application/pdf")
         .author(user)
         .issue(issue)
         .build()
        attachmentManager.createAttachment(bean)
        log.debug "${file.name} attached to ${issue.key}"
        if(file.delete()) log.debug "temp file deleted"
    }
}