import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.mail.Email
import java.text.SimpleDateFormat
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.history.ChangeItemBean
import java.net.URLEncoder

def applicationUser = ComponentAccessor.getUserManager().getUserByName("Admin")
def devopsGroup = ComponentAccessor.groupManager.getUsersInGroup("DEVOPS")
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
StringBuilder body = new StringBuilder()
def cal = Calendar.getInstance()
cal.setTime(new Date()-2)
cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek())
def startOfWeek = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime())
def endOfPreviousWeek = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime().minus(1))
def today = new SimpleDateFormat("yyyy-MM-dd").format(new Date())
def minusDays = cal.getTime() - new Date() -1
def fontStyle = """TEXT-DECORATION: none; FONT-FAMILY: 'Segoe UI'; COLOR: #666666; FONT-STYLE: normal; """
def pStyle = """TEXT-DECORATION: none; FONT-FAMILY: 'Segoe UI'; font-size: 10pt; COLOR: #666666; FONT-STYLE: normal;width:230px; white-space: word-wrap; height:25px;padding:5px 0;"""
def tdStyle = "padding:10px 0; width:40px; height:200px;"
def borderCell ="border-width:1px; border-style: solid;  border-color:#dbd9d9;"
def	tableStyle = """cellspacing='0' style=" table-layout: fixed; ${borderCell} ${fontStyle}" """
List <GString> cellBorderStyle = ["${borderCell} border-right:none;","${borderCell} border-right:none; border-left:none;", "${borderCell} border-right:none; border-left:none;",
                                 "${borderCell} border-right:none; border-left:none;","${borderCell} border-left:none;",
                                 "${borderCell} border-right:none;","${borderCell} border-right:none; border-left:none;", "${borderCell} border-right:none; border-left:none;",
                                 "${borderCell} border-right:none; border-left:none;","${borderCell} border-left:none;",
                                 "${borderCell} border-right:none;","${borderCell} border-right:none; border-left:none;", "${borderCell} border-right:none; border-left:none;",
                                 "${borderCell} border-right:none; border-left:none;","${borderCell} border-left:none;",
                                 "${borderCell} border-right:none;","${borderCell} border-right:none; border-left:none;", "${borderCell} border-right:none; border-left:none;",
                                 "${borderCell} border-right:none; border-left:none;","${borderCell} border-left:none;"]

body.append("""<div>
					<h3 style="${fontStyle} white-space: nowrap; text-align:center;">Статистика по обращениям ИТО за ${cal.get(Calendar.WEEK_OF_YEAR)} неделю </h3>
					<div>
					<table  ${tableStyle}  >
						<tbody>
							<tr style="font-weight: bold; font-size: 16px;">
								<td style="height=400px;width:200px;white-space: word-wrap; text-align: center; ${borderCell} border-bottom:none;">Подразделение</td>
                                <td colspan="5" style="text-align: center; ${borderCell} border-bottom:none;"">Запрос <br>на обслуживание</td>
                                <td colspan="5" style="text-align: center; ${borderCell} border-bottom:none;"">Инфраструктурный <br>инцидент</td>
                                <td colspan="5" style="text-align: center; ${borderCell} border-bottom:none;"">Инцидент</td>
                                <td colspan="5" style="text-align: center; ${borderCell} border-bottom:none;"">ИТОГО</td>
							</tr>
                            <tr style="font-weight: bold; height: 1px; width: 1px;">
								<td style="${borderCell} border-top:none;">&nbsp;</td>
								<td colspan="5" style="${tdStyle} ${borderCell} border-top:none; mso-rotate:90;">
                                <div style= "height=250px;width=250px;transform: rotate(-90deg);">
                                <p style="${pStyle}">Зарегистрировано</p>
								<p style="${pStyle}">Закрыто</p>
								<p style="${pStyle}">Из них Dropped</p>
								<p style="${pStyle}">Открыто на конец недели</p>
								<p style="${pStyle}">Дельта за неделю</p>
                                </div></td>
                                
                                <td colspan="5" style="${tdStyle} ${borderCell} border-top:none; mso-rotate:90;">
                                <div style= "height=250px;width=250px;transform: rotate(-90deg);">
                                <p style="${pStyle}">Зарегистрировано</p>
								<p style="${pStyle}">Закрыто</p>
								<p style="${pStyle}">Из них Dropped</p>
								<p style="${pStyle}">Открыто на конец недели</p>
								<p style="${pStyle}">Дельта за неделю</p>
                                </div></td>
                                <td colspan="5" style="${tdStyle} ${borderCell} border-top:none; mso-rotate:90;">
                                <div style= "height=250px;width=250px;transform: rotate(-90deg);">
                                <p style="${pStyle}">Зарегистрировано</p>
								<p style="${pStyle}">Закрыто</p>
								<p style="${pStyle}">Из них Dropped</p>
								<p style="${pStyle}">Открыто на конец недели</p>
								<p style="${pStyle}">Дельта за неделю</p>
                                </div></td>
                                 <td colspan="5" style="${tdStyle} ${borderCell} border-top:none; mso-rotate:90;">
                                <div style= "height=250px;width=250px;transform: rotate(-90deg);">
                                <p style="${pStyle}">Зарегистрировано</p>
								<p style="${pStyle}">Закрыто</p>
								<p style="${pStyle}">Из них Dropped</p>
								<p style="${pStyle}">Открыто на конец недели</p>
								<p style="${pStyle}">Дельта за неделю</p>
                                </div></td>
                                
                            </tr>
                            """)

def jqls = ["project = SD and issuetype in ('Service Request', 'Typical Request', 'Service request with approvals') and created > ${startOfWeek.toString()} and assignee in ",
            "project = SD and issuetype in ('Service Request', 'Typical Request', 'Service request with approvals') and resolved > ${startOfWeek.toString()} and assignee in ",
            "project = SD and issuetype in ('Service Request', 'Typical Request', 'Service request with approvals') and resolved > ${startOfWeek.toString()} and status was in rejected after ${minusDays.toString()}d and assignee in ",
            "project = SD and issuetype in ('Service Request', 'Typical Request', 'Service request with approvals') and status in (Open, 'Waiting for support', 'In Progress', Rejected) and assignee in ",
            "project = SD and issuetype in ('Service Request', 'Typical Request', 'Service request with approvals') and status was in (Open, 'Waiting for support', 'In Progress', Rejected) before ${endOfPreviousWeek.toString()} after ${endOfPreviousWeek.toString()} and assignee in ",
            "project in (ZAB) and created > ${startOfWeek.toString()} and assignee in ",
            "project in (ZAB) and resolved > ${startOfWeek.toString()} and assignee in ",
            "project in (ZAB) and resolved > ${startOfWeek.toString()} and and status was in rejected after ${minusDays.toString()}d and assignee in ",
            "project in (ZAB) and status in (Open, 'Waiting for support', 'In Progress', Rejected) and assignee in ",
            "project in (ZAB) and status was in (Open, 'Waiting for support', 'In Progress', Rejected) before ${endOfPreviousWeek.toString()} after ${endOfPreviousWeek.toString()}  and assignee in ",
            "project = SD and issuetype=incident and created >=${startOfWeek.toString()}  and assignee in ",
            "project = SD and issuetype=incident and resolved >${startOfWeek.toString()} and assignee in ",
            "project = SD and issuetype=incident and resolved >${startOfWeek.toString()} and status was in rejected after ${minusDays.toString()}d and assignee in ",
            "project = SD and issuetype=incident and status in (Open, 'Waiting for support', 'In Progress', Rejected) and assignee in ",
           	"project = SD and issuetype=incident and status was in (Open, 'Waiting for support', 'In Progress', Rejected) before ${endOfPreviousWeek.toString()} after ${endOfPreviousWeek.toString()} and assignee in "]

List <String> groups = ["name1","name2","name3","name4","name5","name6","name7"]
def groupsMap = ["Группа 1":"name1",
"Группа 2":"name2",
"Группа 3":"name3",
"Группа 4":"name4",
"Группа 5":"name5",
"Группа 6":"name6",
"Группа 7":"name7"]

def baseURL = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") +"/issues/?jql="
ArrayList<ArrayList<Integer>> fullListCounts = new ArrayList<ArrayList<Integer>>()
for (int i =0 ;i< groups.size();i++){ //groups loop
    String memberOf = "membersOf('${groups[i]}')"
    ArrayList<Integer> oneListCounts = new ArrayList<Integer>()
    ArrayList<String> linksToJQL = new ArrayList<String>()
    for(int j=0;j<15;j++){//jqls loop
        def currJQL = jqls[j]+ memberOf
		final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, currJQL)
		final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
		def issues = results.getResults()
        if (j!=4 && j!=9 && j!=14){
            linksToJQL.add(baseURL+ URLEncoder.encode(currJQL)) 
            oneListCounts.add(issues.size())
        } 
        else{
            if (j==4) {
           	 	oneListCounts.add(oneListCounts[3]-issues.size())
            	linksToJQL.add("") 
            }
            if (j==9) {
               	 linksToJQL.add("")
                 oneListCounts.add(oneListCounts[8]-issues.size())
            }
            if (j==14) {
            	oneListCounts.add(oneListCounts[13]-issues.size())
				linksToJQL.add("")
            }
        }
       	
    }
    oneListCounts.add(oneListCounts[0]+oneListCounts[5]+oneListCounts[10])
    oneListCounts.add(oneListCounts[1]+oneListCounts[6]+oneListCounts[11])
    oneListCounts.add(oneListCounts[2]+oneListCounts[7]+oneListCounts[12])
    oneListCounts.add(oneListCounts[3]+oneListCounts[8]+oneListCounts[13])
    oneListCounts.add(oneListCounts[4]+oneListCounts[9]+oneListCounts[14])
    linksToJQL.addAll(["","","","",""])
    fullListCounts.add(oneListCounts) 
    body.append("""<tr><td style="${borderCell} width:200px;white-space: word-wrap;">${groupsMap.find { K,V-> V==groups[i] }.getKey()}</td>""")
    for (int k =0 ;k< 20;k++){
        body.append("""<td style="${cellBorderStyle[k]} text-align:center;">""")
        if (linksToJQL[k]!="") //adding link to JQL tag
        	body.append("""<a href="${linksToJQL[k]}">""")
        if((k==4||k==9||k==14||k==19) && oneListCounts.get(k)>0) //adding color RED
        	 body.append("""<span style="color: #ff0000;">${oneListCounts.get(k)}</span>""")
        else {
            if((k==4||k==9||k==14||k==19) && oneListCounts.get(k)<0) //adding color GREEN
            	body.append("""<span style="color: #00ff00;">${oneListCounts.get(k)}</span>""")
            else // no color
               	body.append("""<span style="color: #000000;">${oneListCounts.get(k)}</span>""")
        }
        if (linksToJQL[k]!="") //closing link to JQL tag
        	body.append("""</a>""")
        body.append("""</td>""")
        
    }    
	body.append("""</tr>""")
}

ArrayList<Integer> summarizeList = new ArrayList<Integer>()
for (int i =0 ;i< 20;i++){
    int sum = 0
    for(int j=0;j<7;j++){
        sum += fullListCounts.get(j).get(i)
    }
    summarizeList.add(sum)
}
body.append("""<tr style="font-weight: bold; font-size: 16px;"><td style="${borderCell}">ИТОГО:</td>""")
for (int k =0 ;k< 20;k++){
        if((k==4||k==9||k==14||k==19) && summarizeList.get(k)>0) body.append("""<td style="${cellBorderStyle[k]} text-align:center;"><span style="color: #ff0000;">${summarizeList.get(k)}</span></td>""")
        else {
            if((k==4||k==9||k==14||k==19) && summarizeList.get(k)<0) body.append("""<td style="${cellBorderStyle[k]} text-align:center;"><span style="color: #00ff00;">${summarizeList.get(k)}</span></td>""")
            else body.append("""<td style="${cellBorderStyle[k]} text-align:center;">${summarizeList.get(k)}</td>""")
        }
    } 
body.append("""</tr></tbody></table></div></div>""")
//second report
body.append("""<br>
				<table ${tableStyle}>
                    <caption><h3>Обращения с аномальными значениями полей Number of Reassignments, Rejects Count за ${cal.get(Calendar.WEEK_OF_YEAR)} неделю</h3></caption>
						<tbody>
							<tr style=" font-weight: bold;">
								<td style="${borderCell}">Number</td>
								<td style="${borderCell}">Creation Date</td>                                
                                <td style="${borderCell}">Reporting Person</td>
								<td style="${borderCell}">Title</td>
								<td style="${borderCell}">Serviсe Catalogue Item</td>
                                <td style="${borderCell}">Responsible</td>
                                <td style="${borderCell}">Rejects Count</td>
                                <td style="${borderCell}">Number of<br> Reassignments</td>
							</tr>""")
List <Issue> strangeIssues = new ArrayList()
Map strangeRejects = new HashMap()
Map strangeReassignments = new HashMap()
String jql =  "project = sd AND updated >= ${startOfWeek.toString()} AND status !=Closed "
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jql)
final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
def issues = results.getResults()
for (def issue : issues){ 
    List<ChangeItemBean> history = ComponentAccessor.changeHistoryManager.getChangeItemsForField(issue, "status").findAll{it.toString=="Rejected"}
    if (history.size()>1){
        strangeIssues.add(issue)
        strangeRejects.put(issue.key,history.size())
    }    
    history = ComponentAccessor.changeHistoryManager.getChangeItemsForField(issue, "assignee")
    if (history.size()>3){
        strangeIssues.add(issue)
        strangeReassignments.put(issue.key,history.size())
    }
}
def itServiceCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("IT Service").first()
for (def issue : strangeIssues){ 
    String rejects = "0"
    String reassignments = "0"
    String service = "Empty"
    def key = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") +"/browse/" +issue.key
    if (itServiceCF.getValue(issue)) service = itServiceCF.getValue(issue).first().toString()
    if (strangeRejects.getAt(issue.key)) rejects = strangeRejects.getAt(issue.key).toString()
    if (strangeReassignments.getAt(issue.key)) reassignments = strangeReassignments.getAt(issue.key).toString()
    if (strangeReassignments.getAt(issue.key)) reassignments = strangeReassignments.getAt(issue.key).toString()
    body.append("""<tr>
						<td style="${borderCell} text-align:center;"><a title="${issue.key}" href="${key}">${issue.key}</a></td>
						<td style="${borderCell} text-align:center;">${new SimpleDateFormat("dd.MM.yyyy").format(issue.created)}</td>                                
                        <td style="${borderCell}"><p style=" width:110px; word-wrap: break-word;">${issue.reporter.displayName}</p></td>
						<td style="${borderCell}"><p style=" width:300px; word-wrap: break-word;">${issue.summary}</p></td>
						<td style="${borderCell}"><p style=" width:300px; word-wrap: break-word;">${service}</p></td>
                        <td style="${borderCell}"><p style=" width:110px; word-wrap: break-word;">${issue.assignee.displayName}</p></td>
                        <td style="${borderCell} text-align:center;">${rejects}</td>
                        <td style="${borderCell} text-align:center;">${reassignments}</td>
					</tr>""")
}
body.append("""<tr style="font-weight: bold; font-size: 16px;"><td style="${borderCell}">ИТОГО:</td>
								<td colspan="7" style="${borderCell}">${strangeIssues.size()}</td>                                
							</tr>""")
return body