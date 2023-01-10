import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.workflow.JiraWorkflow
import com.atlassian.jira.workflow.WorkflowSchemeManager
import com.atlassian.jira.scheme.Scheme
import org.apache.log4j.Logger
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.issue.fields.screen.DefaultFieldScreenSchemeManager
import com.atlassian.jira.issue.fields.screen.FieldScreen
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme

def log = Logger.getLogger("com.acme.workflows")
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey("JIRA_Service")

def projects  = ComponentAccessor.projectManager.getProjectObjects()
List<String> uselessProject = new ArrayList()
projects.each {    
    	def issuesCount = ComponentAccessor.issueManager.getIssueCountForProject(it.getId())
    	if (issuesCount <10) uselessProject.add(it.getKey())
    }

WorkflowSchemeManager wfScManager = ComponentAccessor.getWorkflowSchemeManager()
def wfScAll = wfScManager.schemeObjects //получаем все wf схемы
def wfScUseless = wfScManager.schemeObjects // отсюда будут удаляться используемые схемы, и остануться только не используемые
for (def wfSc: wfScAll){
    def schemeProjects =   wfScManager.getProjects(wfSc) //получаем список проектов привязаных к схеме
    schemeProjects.removeAll{project -> uselessProject.findAll{it==project.getKey()}}//игнорируем слабоиспользуемые проекты
    if (schemeProjects.size()>0) { //если что то осталось, то схема используемая и убирается из списка бесполезных схем
        wfScUseless.remove(wfSc)
    //    log.warn("good scheme "+wfSc.getName()+" "+schemeProjects.size())
    }
}

Collection<JiraWorkflow> workflowsAll = ComponentAccessor.getWorkflowManager().getWorkflows() //получаем все wf
List<JiraWorkflow> inactiveWF  = new ArrayList() //лист для хранения wf, которые не используются нигде
List<JiraWorkflow> inactiveWFwithScheme = new ArrayList() //лист для хранения wf, которые не используются, но прикреплены к бесполезной схеме
List<JiraWorkflow> goodWFs = new ArrayList() // лист для хранения wf, которые активно используются
List<JiraWorkflow> workflowsCopies = new ArrayList() //лист для хранения wf, при поиске копий 
List<JiraWorkflow> uselessWF = new ArrayList() // лист для хранения бесполезных wf
for (def wf : workflowsAll){
    def wfSchemes = wfScManager.getSchemesForWorkflow(wf).toList()  // получаем схемы куда прикреплён wf
    if (wfSchemes.size()>0){
       // log.warn(wf.getName()+" have schemes "+ wfSchemes.size())
        wfSchemes.removeAll{scheme->wfScUseless.findAll{it.id==scheme.id}} //исключаем бесполезные схемы
        if (wfSchemes.size()>0){ // если что то осталось ложим в хорошие wf
            //log.warn(wf.getName()+" is good wf")
            goodWFs.add(wf)
        }
        else { // если схем не осталось, то значит wf прикреплен только к бесполезным схемам и считается бесполезным
            //log.warn(wf.getName()+" but these schemes is inactive")
            inactiveWFwithScheme.add(wf) 
            uselessWF.add(wf)
        }
    }
    else{ // если схем нет, то значит wf не активный и считается бесполезным
        //log.warn(wf.getName()+" is fully inactive")
        inactiveWF.add(wf)
        uselessWF.add(wf)
    }    
}


Map mapOfCopies = new HashMap() //поиск одинаковых по статусам wf 

for (def wf1 : goodWFs){
    if (!workflowsCopies.find{it==wf1}){ //исключаем уже отработаные wf 
        List<String> statusIds1 = new ArrayList<>(wf1.linkedStatusIds) //получаем статусы главного wf
        for (def wf2 : goodWFs){
            if (wf1==wf2 || workflowsCopies.find{it==wf2}) continue //исключаем уже отработаные wf и текущий
            List<String> statusIds2 = new ArrayList<>(wf2.linkedStatusIds) //получаем статусы проверяемого wf
           	statusIds2.removeAll(statusIds1) // удаляем все статусы, которые есть у главного
            if (statusIds2.size()==0) { // если статусов не осталось, то проверяемый wf можно полностью заменить главным
                //log.warn(wf1.getName()+" ----- " + wf2.getName())
                workflowsCopies.add(wf2) // ложим проверяемый в отработаные wf 
                uselessWF.add(wf2) // так как проверяемый wf можно полностью заменить главным, то можно считать его бесполезным
                String currentValue = mapOfCopies.get(wf1.getName())
                if (!currentValue) currentValue = wf2.getName()
                else currentValue = currentValue +", "+wf2.getName()
                mapOfCopies.put(wf1.getName(), currentValue ) // ложим в словарь главный wf и его копию
            }
        }
        workflowsCopies.add(wf1) // ложим главный в отработаные wf 
    }    
}
//screens
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
for (def wf : goodWFs){
    for (def screen : screensAll)
        if (!usedScreens.find{it==screen} && wf.getActionsForScreen(screen)) usedScreenSchemes.add(screen)
}

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

def mapOfUsage = new HashMap() 
for (def wf : goodWFs){
   // log.warn "Check usage for workflow:  ${wf.getName()}"
    def wfSchemes = wfScManager.getSchemesForWorkflow(wf)   // получаем схемы куда прикреплён wf
    def jqlForWF =""
    def temp =""
    for (def schemeTemplate: wfSchemes){   
        if (wfScUseless.find{it.id==schemeTemplate.id}) continue  //пропускаем бесполезные схемы
        Scheme scheme = wfScManager.getSchemeObject(schemeTemplate.id)
       // log.warn "Check scheme for :  ${scheme.getName()}"
        List<String> wfProjects = new ArrayList()  //лист с проектами куда прикреплена схема
    	List<String> wfIssueTypes = new ArrayList()  //лист типов задач к которым прикреплена схема
        def avaibleIssuetypes = new ArrayList()  // лист доступных типов задач для проектов куда прикреплена схема
        //log.warn(wf.getName()+" used in projects: " + wfScManager.getProjects(scheme).key)
       	wfProjects.addAll( wfScManager.getProjects(scheme).key)   //получаем проекты
        if (!wfProjects) continue
        wfProjects.each{  projectkey->
		//получаем все доступные типы задач в проектах схемы
            avaibleIssuetypes.addAll(ComponentAccessor.projectManager.getProjectByCurrentKey(projectkey).getIssueTypes())
            StringBuilder issueTypesForJQL = new StringBuilder();
			def entities = scheme.entities //получаем связки issuetype+wf
			def entitiesForWF = entities.findAll { it.entityTypeId.equals(wf.getName())  } //фильтруем связки только для текущего wf
			def issueTypesParameters =  entitiesForWF.parameter //получаем список issuetype id 
			if (issueTypesParameters.find{it=='0'}) { // wf является стандартным, значит применяется для всех у кого нет определенного wf
				//log.warn(wf.getName()+" is default and avaible Issue types: " + avaibleIssuetypes.name )
				def otherEntities = entities.findAll { !it.entityTypeId.equals(wf.getName())  }.parameter //получаем список issuetype id, которые не относятся к нашему wf
				if (otherEntities){
					otherEntities.each { id->
                        if(avaibleIssuetypes.find{it.id!=id.toString()})
							wfIssueTypes.add(avaibleIssuetypes.find{it.id!=id.toString()}.name)
					}
				}
				else wfIssueTypes.addAll(avaibleIssuetypes.name)
            }
			else {
				issueTypesParameters.each{id->
                    if(avaibleIssuetypes.find{it.id==id.toString()})
						wfIssueTypes.add(avaibleIssuetypes.find{it.id==id.toString()}.name)
				}
			}        
			if (temp!="") temp = temp + " OR "
            wfIssueTypes.each{
                if (issueTypesForJQL.length()!=0)    issueTypesForJQL.append(',')  
                issueTypesForJQL.append('"')
                issueTypesForJQL.append(it)
                issueTypesForJQL.append('"')
            }
			temp =temp+ "(project in (${wfProjects.toString().replaceAll("\\[","").replaceAll("\\]","")}) and  issuetype in (${issueTypesForJQL}))" 
		}			
    }    
    jqlForWF = jqlForWF+temp //делаем запрос и получаем колво тикетов которые используют данный wf
    //log.warn jqlForWF
    final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlForWF)
    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    def issuesCount = results.getIssues().size()
    //log.warn  "${wf.name} have ${issuesCount} issues"	
	mapOfUsage.put(issuesCount,wf.name)
    if (issuesCount==0) uselessWF.add(wf)//если wf не используется, то можно считать его бесполезным
    
}

//log.warn mapOfCopies
//log.warn mapOfUsage
//return wfScUseless.size() //получаем бесполезные схемы
//return inactiveWFwithScheme.size()
//return inactiveWF.size()
//return uselessWF.size()
//return goodWFs.name
//goodWFs.removeAll(uselessWF)
//log.warn goodWFs.size()
StringBuilder body = new StringBuilder()
body = parseListToBody(body,inactiveWF.name,"Inactive Workflows")
body = parseListToBody(body,inactiveWFwithScheme.name,"Inactive with shemes Workflows")
body = parseListToBody(body,wfScUseless.name,"Useless Workflows Schemes")
body = parseListToBody(body,uselessWF.name,"Useless Workflows")
mapOfCopies.each{K,V->
    body = parseListToBody(body,mapOfCopies.get(K).toString().split(","),"Copies of ${K}")
}
body.append("\n")
body.append("----------------------------------------------------")
body.append("Usage of good workflows")
body.append("----------------------------------------------------")
body.append("\n")
mapOfUsage.sort().each{K,V->    
    if (K>0){
    	body.append("${V} have ${K} issues. ")
        body.append("\n")
    }
}
body = parseListToBody(body,uselessIssueTypeScreenSchemes.name,"Useless Issue Type Screen Schemes")
body = parseListToBody(body,uselessScreenSchemes.name,"Useless Screen Schemes")
body = parseListToBody(body,uselessScreens.name,"Useless Screens")
mapOfCopiesScreens.each{K,V->
    body = parseListToBody(body,mapOfCopiesScreens.get(K).toString().split(","),"Copies of ${K}")
}
def jiraHome = ComponentAccessor.getComponent(JiraHome)
def file = new File(jiraHome.home, "WF_analyze.txt") //location JIRA_HOME/WF_analyze.txt
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