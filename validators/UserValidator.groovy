import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.user.search.UserSearchService
import org.apache.log4j.Level
import org.apache.log4j.Logger
import com.opensymphony.workflow.InvalidInputException

//format for validation "userName,email,fullName"
Logger logger = Logger.getLogger("UserCreationValidator")
logger.setLevel(Level.INFO)
def cf = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Перечень пользователей").first()
String value = cf.getValue(issue).toString()
boolean check = false
if (value){
    try {
        def list = value.split("\n")        
        if (list){
            for (def newUser : list){
                String checka = checkOneLine(newUser,logger)
                if (checka && checka=="true"){
                    check=true
                }
                else if (checka && checka!="default"){
                    invalidInputException = new InvalidInputException("Перечень пользователь",checka)
                    return
                }
            }
	    }
        //try handle one line
        else {
            String checka = checkOneLine(value,logger)
            if (checka && checka=="true"){
                check=true
            }
            else if (checka && checka!="default"){
                invalidInputException = new InvalidInputException("Перечень пользователь",checka)
                return
            }
        }
    }
    catch(Exception ex){
    	invalidInputException = new InvalidInputException("Перечень пользователь","${value}\nВы нарушили шаблон! userName,email,fullName") 
        logger.info("${ex}")
        return
    }
    if (!check)
		invalidInputException = new InvalidInputException("Перечень пользователь","Введите данные!")
}
String checkOneLine(String newUser, Logger logger){
    def serchService = ComponentAccessor.getComponent(UserSearchService.class)
    def values = newUser.trim().split(",")
    String userName = values[0].trim()
    String email = values[1].trim()
    String fullName = values[2].trim()   
    //ignore default values
    if (userName == "userName" || email == "email" || fullName == "fullName")
        return "default"
    logger.info(userName)
    logger.info(email)
    logger.info(fullName)
    //username validation
    if (userName.find("[^a-zA-Z|.|0-9|\\_]")){
        logger.info("""userName "${userName}" invalid""")
        return """Имя пользователя "${userName}" недействительно"""
    }
    if (userName.size()>30){
        logger.info("""userName "${userName}" too long""") 
        return """Имя пользователя "${userName}" слишком длинное"""
    }
    if (ComponentAccessor.userManager.getUserByName(userName)){
        logger.info("""userName "${userName}" already in use""") 
        return """Имя пользователя "${userName}" уже используется"""
    }
    //email validation
    if (!email.find("""^[\\w!#\$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#\$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}\$""")){
        logger.info("""email "${email}" invalid""") 
        return """Электронная почта "${email}" недействительна"""
    }
    if (serchService.findUsersByEmail(email)){
        logger.info("""email "${email}" already in use""") 
        return """Электронная почта "${email}" уже используется"""
    }
    //fullname validation
    if (fullName.find("[^a-zA-Z|а-яА-Я|\\ ]")){
        logger.info("""fullName "${fullName}" invalid""" ) 
        return """Полное имя "${fullName}" недействительно"""
    }
    return "true"
}
