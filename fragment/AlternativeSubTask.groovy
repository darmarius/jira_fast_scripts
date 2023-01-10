import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.sal.api.ApplicationProperties
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import com.atlassian.jira.issue.MutableIssue

MutableIssue issue = (MutableIssue)context.issue
def subtasks = issue.getSubTaskObjects()
StringBuffer sb = new StringBuffer()
sb.append("""<h2>SubTasks</h2>
    <table class="aui">
       <thead>
        <tr>
            <th>Key</th>
            <th>Components</th>
            <th>Status</th>
            <th>Assignee</th>
            <th>Product Group</th>
            <th>Subsystem</th>
            <th>Activity</th>
        </tr>
    </thead>
    <tbody>""")
for (def sub : subtasks){
   	def  componet,assignee, productGR, subsystem = ""
    def productGRcf = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Product Group")
    def subsystemcf = ComponentAccessor.customFieldManager.getCustomFieldObjectByName("Subsystem")
   	if (sub.components.size()>0) componet = sub.components.toString()
   	if (sub.assignee) assignee = sub.assignee
   	if (productGRcf.getValue(issue)) productGR = productGRcf.getValue(issue).value
   	if (subsystemcf.getValue(issue)) subsystem = subsystemcf.getValue(issue).value
    sb.append("""<tr>
                        <td>${sub.getKey()}</td>
                        <td>${componet}</td>
                        <td>${sub.status.name}</td>
                        <td>${assignee}</td>
                        <td>${productGR}</td>
                        <td>${subsystem}</td>
                        <td>Activity from ${sub.getKey()}</td>
				</tr>""")
}
sb.append("</tbody></table>")  
   
writer.write(sb.toString())