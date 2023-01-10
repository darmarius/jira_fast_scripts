import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.fields.screen.DefaultFieldScreenSchemeManager
import com.atlassian.jira.issue.fields.screen.FieldScreen
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme

def issueTypeScreenSchemesAll = ComponentAccessor.getIssueTypeScreenSchemeManager().getIssueTypeScreenSchemes()
def goodIssueTypeScreenSchemes = new ArrayList()
def uselessIssueTypeScreenSchemes = new ArrayList()
List<FieldScreenScheme> usedScreenSchemes = new ArrayList()
for (def itss : issueTypeScreenSchemesAll){
	if(!itss.getProjects())  uselessIssueTypeScreenSchemes.add(itss)
    else {
        goodIssueTypeScreenSchemes.add(itss)
        itss.getEntities().each{
            usedScreenSchemes.add(it.fieldScreenScheme)
        }
    }    
}

def screenSchemeManager = ComponentAccessor.getComponent(DefaultFieldScreenSchemeManager)
def screenSchemesAll = screenSchemeManager.getFieldScreenSchemes()
def uselessScreenSchemes = screenSchemesAll
List<FieldScreen> usedScreens = new ArrayList()
uselessScreenSchemes.removeAll(usedScreenSchemes)
for (def ss : usedScreenSchemes){
    ss.getFieldScreenSchemeItems().each { fssi->
    	usedScreens.add(fssi.getFieldScreen())
    }
}

List<FieldScreen> screensAll = (List<FieldScreen>)ComponentAccessor.getFieldScreenManager().getFieldScreens().toList()
List<FieldScreen> uselessScreens = (List<FieldScreen>)ComponentAccessor.getFieldScreenManager().getFieldScreens().toList()
uselessScreens.removeAll(usedScreens)
def screensCopyCheck = new ArrayList()
Map mapOfCopiesScreens = new HashMap()
for (def screen1 : screensAll){
    if(screensCopyCheck.find{it==screen1})continue
    for (def screen2 : screensAll){        
        if (screen1!=screen2 && !screensCopyCheck.find{it==screen2} && screen1.tabs.size()==screen2.tabs.size()){
            def sameTabs= 0           
            for (def i=0;i<screen1.tabs.size();i++){                
            	def cfScopeOnTab = screen1.tabs[i].getFieldScreenLayoutItems().fieldId                
                cfScopeOnTab.removeAll(screen2.tabs[i].getFieldScreenLayoutItems().fieldId)                
                if (cfScopeOnTab.isEmpty()) sameTabs++
            }            	  
            if (screen1.tabs.size()==sameTabs){
                screensCopyCheck.add(screen2)
                if(!uselessScreens.find{it==screen2}) uselessScreens.add(screen2)
                String currentValue = mapOfCopiesScreens.get(screen1.getName())
                if (!currentValue) currentValue = screen2.getName()
                else currentValue = currentValue +", "+screen2.getName()
                mapOfCopiesScreens.put(screen1.getName(), currentValue ) // ложим в словарь главный screen и его копию
            }
        }
        else continue
    }
    screensCopyCheck.add(screen1)
}

StringBuilder body = new StringBuilder()
body = parseListToBody(body,uselessIssueTypeScreenSchemes.name,"Useless Issue Type Screen Schemes")
body = parseListToBody(body,uselessScreenSchemes.name,"Useless Screen Schemes")
body = parseListToBody(body,uselessScreens.name,"Useless Screens")
mapOfCopiesScreens.each{K,V->
    body = parseListToBody(body,mapOfCopiesScreens.get(K).toString().split(","),"Copies of ${K}")
}

def jiraHome = ComponentAccessor.getComponent(JiraHome)
def file = new File(jiraHome.home, "Screens_analyze.txt") //location JIRA_HOME/Screens_analyze.txt
file.write body.toString()

StringBuilder parseListToBody (StringBuilder body,def source, String name){
	body.append("\n")
	body.append("----------------------------------------------------")
	body.append(name)
	body.append("----------------------------------------------------")
	body.append("\n")
	def counter = 0
	source.each{
		body.append(it)
		body.append(", ")
		counter++
		if (counter == 5){
			counter=0
			body.append("\n")
		}        	
	}
	body.append("\n")
	body.append("-----------------------------------------------------------------------------------------------------------------------------------")
	body.append("\n")
	return body
}