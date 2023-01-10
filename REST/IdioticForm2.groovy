import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.UrlMode
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.TransitionOptions.Builder
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonOutput

@BaseScript CustomEndpointDelegate delegate

moveToanotherSD(httpMethod: "GET") { MultivaluedMap queryParams ->
    String key = ((String)queryParams.get("key").first()).replace("[","").replace("]","")
    String targetProject = ((String)queryParams.get("project").first()).replace("[","").replace("]","")
    String action = ((String)queryParams.get("action").first()).replace("[","").replace("]","")
    def baseUrl = ScriptRunnerImpl.getOsgiService(ApplicationProperties).getBaseUrl(UrlMode.ABSOLUTE)
    if (action == "action"){
        String comment = ((String)queryParams.get("comment").first()).replace("[","").replace("]","")
        /*
		move issue
		*/
     	def flag = [
        	type : 'success',
        	title: "Issue moved",
        	close: 'auto',
        	body : "Тикет перемещен в проект ${targetProject} c новым ключом ${newTask.key}"
    	]
    	Response.ok(JsonOutput.toJson(flag)).build()
    }
    else{
	String dialog =        """
        <script>
    function submit() {
    	var element = document.querySelector("#comment-move");
        var url = "${baseUrl}/rest/scriptrunner/latest/custom/moveToanotherSD?key=${key}&project=${targetProject}&action=action&comment="+element.value
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
		var inputElement = document.querySelector("#comment-move");
        if (inputElement){
        	if (inputElement.value!=""){
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
    <h2 class="aui-dialog2-header-main">Move issue to ${targetProject}</h2>
</header>
<div class="aui-dialog2-content">
    <p>Please comment your decision to move issue to ${targetProject}</p>
    <form class="aui" action="#">
        <label for="comment-move">Comment</label>
        <textarea class="textarea" name="comment-input" id="comment-move" placeholder="Your comment here..."></textarea>
            </form>
</div>
<footer class="aui-dialog2-footer">
    <div class="aui-dialog2-footer-actions">
        <button class="aui-button" id="submit" resolved="" style="visibility: hidden;">Move Issue</button>
        <button id="dialog-close-button" class="aui-button aui-button-link">Close</button>
    </div>
</footer>
</section>
        """
    Response.ok().type(MediaType.TEXT_HTML).entity(dialog.toString()).build()    
    }
}
