import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.user.search.UserSearchService
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript
import org.apache.log4j.Level
import org.apache.log4j.Logger

@BaseScript FieldBehaviours fieldBehaviours

//format for validation "userName,email,fullName"

Logger log = Logger.getLogger("UserCreationValidator")
log.setLevel(Level.INFO)

def cf = getFieldByName("Перечень пользователей")
String value = cf.getFormValue()
boolean check = false
if (value){
    try {
        def list = value.split("\n")        
        if (list){
            for (def newUser : list){
                def serchService = ComponentAccessor.getComponent(UserSearchService.class)
                def values = newUser.trim().split(",")
                String userName = values[0].trim()
                String email = values[1].trim()
                String fullName = values[2].trim()   
                //ignore default values
                if (userName == "userName" || email == "email" || fullName == "fullName")
                    continue
                log.info(userName)
                log.info(email)
                log.info(fullName)
                //username validation
                if (userName.find("[^a-zA-Z|.|0-9|\\_]")){
                    cf.setError("""Имя пользователя "${userName}" недействительно""")
                    log.info("""userName "${userName}" invalid""")
                    return
                }
                if (userName.size()>30){
                    cf.setError("""Имя пользователя "${userName}" слишком длинное""") 
                    log.info("""userName "${userName}" too long""") 
                    return
                }
                if (ComponentAccessor.userManager.getUserByName(userName)){
                    cf.setError("""Имя пользователя "${userName}" уже используется""") 
                    log.info("""userName "${userName}" already in use""") 
                    return
                }
                //email validation
                if (!email.find("""^[\\w!#\$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#\$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}\$""")){
                    cf.setError("""Электронная почта "${email}" недействительна""") 
                    log.info("""email "${email}" invalid""") 
                    return
                }
                if (serchService.findUsersByEmail(email)){
                    cf.setError("""Электронная почта "${email}" уже используется""") 
                    log.info("""email "${email}" already in use""") 
                    return
                }
                //fullname validation
                if (fullName.find("[^a-zA-Z|а-яА-Я|\\ ]")){
                    cf.setError("""Полное имя "${fullName}" недействительно""" ) 
                    log.info("""fullName "${fullName}" invalid""" ) 
                    return
                }
                check = true
            }
        }
	}
    catch(Exception ex){
    	cf.setError("<p>Вы нарушили шаблон!</p> <p>userName,email,fullName</p>") 
        log.info("${ex}")
    }
}
else	cf.setError("Заполните пользователей по шаблону!")
if (check)	cf.clearError()