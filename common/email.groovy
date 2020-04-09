import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.mail.Email

StringBuilder body = new StringBuilder()
body.append("<h1>bla bla bla</h1>")

def mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()
if (mailServer) {    
    Email mail = new Email("email1@gmail.com,email2@gmail.com")
  	mail.setSubject("Subject")
   	mail.setBody(body.toString())    	
    mail.setMimeType("text/html")
   	mailServer.send(mail)
}