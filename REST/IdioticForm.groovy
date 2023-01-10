import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.UrlMode
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import groovy.json.JsonOutput

@BaseScript CustomEndpointDelegate delegate

idioticForm(httpMethod: "GET") { MultivaluedMap queryParams ->
    String key = ((String)queryParams.get("key").first()).replace("[","").replace("]","")
    String action = ((String)queryParams.get("action").first()).replace("[","").replace("]","")
    def baseUrl = ScriptRunnerImpl.getOsgiService(ApplicationProperties).getBaseUrl(UrlMode.ABSOLUTE)
    if (action == "action"){
        String comment = ""
        if(queryParams.get("comment"))
        	comment = ((String)queryParams.get("comment").first()).replace("[","").replace("]","")
        String options = ((String)queryParams.get("options").first()).replace("[","").replace("]","")
        MutableIssue currentIssue = ComponentAccessor.issueManager.getIssueObject(key)
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    	MutableIssue newTask = ComponentAccessor.getIssueFactory().getIssue()
		newTask.setSummary(options)
		newTask.setProjectObject(currentIssue.projectObject)
        newTask.setParentObject(currentIssue)
		newTask.setDescription(comment)
		newTask.setReporter(user)
        newTask.setIssueTypeId(ComponentAccessor.getConstantsManager().getAllIssueTypeObjects().find{it.getName() == "Sub-task"}.id)
    	Map<String,Object> newIssueParams = ["issue" : newTask] as Map<String,Object>
    	ComponentAccessor.getIssueManager().createIssueObject(user, newIssueParams)
        ComponentAccessor.getSubTaskManager().createSubTaskIssueLink(currentIssue, newTask, user)
        def flag = [
        	type : 'success',
        	title: "Issue moved",
        	close: 'auto',
        	body : "Ваше обращение переведено в ${currentIssue.projectObject.getName()} c ключом ${newTask.key}"
    	]
    	Response.ok(JsonOutput.toJson(flag)).build()
    }
    else{
	String dialog =        """
        <script>
    function submit() {
    	var url = "${baseUrl}/rest/scriptrunner/latest/custom/idioticForm?key=${key}&action=action"
    	var element = document.querySelector("#comment-move");
        var check1 = document.querySelector("#checkBoxOne");
        var check2 = document.querySelector("#checkBoxTwo");
        var check3 = document.querySelector("#checkBoxThree");
        var check4 = document.querySelector("#checkBoxFour");
        var optionsChecked = 0;
        if(element.value)
        	url = url + "&comment=" + element.value;
        if(check1.checked == true){
        	url = url + "&options=Wrong Assignment";
            optionsChecked++;
        }
        if(check2.checked == true){
        	if(optionsChecked == 0) url = url + "&options=No (Low Quality) SD Primary Processing";
            else url = url + " - No (Low Quality) SD Primary Processing";
        	optionsChecked++;
        }
        if(check3.checked == true){
        	if(optionsChecked == 0) url = url + "&options=Wrong Service Catalogue Item";
            else url = url + " - Wrong Service Catalogue Item";
        	optionsChecked++;
        }
        if(check4.checked == true){
        	if(optionsChecked == 0) url = url + "&options=Wrong Ticket Classification";
            else url = url + " - Wrong Ticket Classification";
        	optionsChecked++;
        }
        var xhttp = new XMLHttpRequest();
        xhttp.open("GET", url, true);
        xhttp.setRequestHeader("Content-type", "application/json");
        xhttp.send();
        document.querySelector("#sr-dialog").hidden=true;
        location.reload();
    }
    var el = document.getElementById("submit");
    if (el.addEventListener)
        el.addEventListener("click", submit, false);
    else if (el.attachEvent)
        el.attachEvent('onclick', submit);
        
function newpage(){
	var int = setInterval(function(){
        var check1 = document.querySelector("#checkBoxOne");
        var check2 = document.querySelector("#checkBoxTwo");
        var check3 = document.querySelector("#checkBoxThree");
        var check4 = document.querySelector("#checkBoxFour");
        if (check1 && check2 && check3 && check4){
        	if (check1.checked == true || check2.checked == true || check3.checked == true || check4.checked == true){
            	var el = document.getElementById("submit");
                el.style.visibility="";
                clearInterval(int);
            }
        }
        else clearInterval(int);
	}, 20);
}
var docOnLoad = setInterval(function(){
AJS.\$(document).ready(function(\$){newpage();}); 
}, 100);
</script>
        <section role="dialog" id="sr-dialog"    class="aui-layer aui-dialog2 aui-dialog2-medium" aria-hidden="true" data-aui-remove-on-hide="true">
<header class="aui-dialog2-header">
    <h2 class="aui-dialog2-header-main">Quality Evaluation of ${key}</h2>
</header>
<div class="aui-dialog2-content">
<fieldset class="group">
    <h3><legend><span>Process Evaluation</span></legend></h3>
    <div class="checkbox">
        <input class="checkbox" type="checkbox" name="checkBoxOne" id="checkBoxOne">
        <label for="checkBoxOne">Wrong Assignment</label>
    </div>                                
    <div class="checkbox">
        <input class="checkbox" type="checkbox" name="checkBoxTwo" id="checkBoxTwo">
        <label for="checkBoxTwo">No (Low Quality) SD Primary Processing</label>
    </div>                                
    <div class="checkbox">
        <input class="checkbox" type="checkbox" name="checkBoxThree" id="checkBoxThree">
        <label for="checkBoxThree">Wrong Service Catalogue Item</label>
    </div>
    <div class="checkbox">
        <input class="checkbox" type="checkbox" name="checkBoxFour" id="checkBoxFour">
        <label for="checkBoxThree">Wrong Ticket Classification</label>
    </div> 
</fieldset>
    <form class="aui" action="#">
        <label for="comment-move"><h3>Comment</h3></label>
        <textarea class="textarea" name="comment-input" id="comment-move" placeholder="Your comment here..."></textarea>
            </form>
</div>
<footer class="aui-dialog2-footer">
    <div class="aui-dialog2-footer-actions">
        <button class="aui-button" id="submit" resolved="" style="visibility: hidden;">Create Issue</button>
        <button id="dialog-close-button" class="aui-button aui-button-link">Close</button>
    </div>
</footer>
</section>
        """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()    
    }
}
