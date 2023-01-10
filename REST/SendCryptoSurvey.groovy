import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.util.UserManager
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.mail.Email
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.config.util.JiraHome
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import org.apache.log4j.Level
import org.apache.log4j.Logger
import java.text.SimpleDateFormat
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import com.atlassian.mail.queue.SingleMailQueueItem

Class_SatisfactionSender script = new Class_SatisfactionSender()
script.run(issue)

class Class_SatisfactionSender{
    Logger log = Logger.getLogger(this.class)
    UserManager userManager = ComponentAccessor.getUserManager()
    UserUtil userUtil = ComponentAccessor.getUserUtil()
    String jiraHome = ComponentAccessor.getComponent(JiraHome).getHome()
    String templatePath = "${jiraHome}/template.html"
    String templatePathRu = "${jiraHome}/templateRU.html"
    private static String password = "1234"
    private static String salt = "4321"
    private static String vector = "2233"

    def run(MutableIssue issue){
        log.setLevel(Level.DEBUG)
        long scriptStartTime = System.currentTimeMillis()
        log.debug("Script started")

        File templateFile = new File(templatePath)
        String userLocale = ComponentAccessor.localeManager.getLocaleFor(issue.reporter).toString()
        if (userLocale=="ru_RU") templateFile = new File(templatePathRu)
        if (templateFile.exists()){
            //Email variables
            String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
            String issueKey = issue.key
            String issueSummary = issue.summary
            String issueResolution = "Unresolved"
            if (issue.resolution) {issueResolution = issue.resolution.getName()}
            String issueLink = baseUrl+"/browse/"+issueKey
            String satisfactionURL = "https://client.com/en/satisfaction?token="
            if (userLocale=="ru_RU") satisfactionURL = "https://client.com/satisfaction?token="
            String dateCreated =  new SimpleDateFormat("dd/MM/yyyy").format(new Date())

            //Getting template text
            String fileContent = templateFile.text

            //Getting MD5 for issue
            String hash = getHash(issue)

            //Setting body
            String tokenOne = "${issue.id}:${issue.reporter.getId()}:1:${hash}:${dateCreated}"
            String tokenOneEncoded = URLEncoder.encode(encrypt(tokenOne), "UTF-8")
            String oneStarLink = satisfactionURL+tokenOneEncoded

            String tokenTwo = "${issue.id}:${issue.reporter.getId()}:2:${hash}:${dateCreated}"
            String tokenTwoEncoded = URLEncoder.encode(encrypt(tokenTwo), "UTF-8")
            String twoStarLink = satisfactionURL+tokenTwoEncoded

            String tokenThree = "${issue.id}:${issue.reporter.getId()}:3:${hash}:${dateCreated}"
            String tokenThreeEncoded = URLEncoder.encode(encrypt(tokenThree), "UTF-8")
            String threeStarLink = satisfactionURL+tokenThreeEncoded

            String tokenFour = "${issue.id}:${issue.reporter.getId()}:4:${hash}:${dateCreated}"
            String tokenFourEncoded = URLEncoder.encode(encrypt(tokenFour), "UTF-8")
            String fourStarLink = satisfactionURL+tokenFourEncoded

            String tokenFive = "${issue.id}:${issue.reporter.getId()}:5:${hash}:${dateCreated}"
            String tokenFiveEncoded = URLEncoder.encode(encrypt(tokenFive), "UTF-8")
            String fiveStarLink = satisfactionURL+tokenFiveEncoded

            String body = fileContent
                    .replace('{issueKey}', issueKey)
                    .replace('{issueSummary}', issueSummary)
                    .replace('{issueLink}', issueLink)
                    .replace('{oneStarLink}', oneStarLink)
                    .replace('{twoStarLink}', twoStarLink)
                    .replace('{threeStarLink}', threeStarLink)
                    .replace('{fourStarLink}', fourStarLink)
                    .replace('{fiveStarLink}', fiveStarLink)
            String subject = "Customer satisfaction survey for ${issueKey}"
            if (issue.reporter.getEmailAddress() != null){
                String toAddress = issue.reporter.getEmailAddress()
                sendMail(toAddress, subject,body)
            }
        }
        else{
            log.debug("File ${templatePath} not found")
        }


        long scriptWorkTime = System.currentTimeMillis() - scriptStartTime
        log.debug("Script work time: ${scriptWorkTime} ms.")
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

    String getHash(MutableIssue issue){
        String issueInfo = issue.key + issue.id + password
        MessageDigest md = MessageDigest.getInstance("MD5")
        md.update(issueInfo.getBytes())
        byte[] digest = md.digest()
        String hash = DatatypeConverter.printHexBinary(digest).toUpperCase()
        return hash
    }
    String encrypt(String originalString){
        IvParameterSpec ivspec = new IvParameterSpec(vector.bytes)
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128)
        SecretKey tmp = factory.generateSecret(spec)
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(),"")
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec)
        String encryptedString =  Base64.getEncoder().encodeToString(cipher.doFinal(originalString.getBytes("UTF-8")))
        return encryptedString
    }
}