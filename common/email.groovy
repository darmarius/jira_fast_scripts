import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.mail.Email

void sendEmail(String recipients, String body){
	def mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()
	if (mailServer) {    
		Email mail = new Email(recipients)
		mail.setSubject("Subject")
		mail.setBody(body)    	
		mail.setMimeType("text/html")
		mailServer.send(mail)
	}
}