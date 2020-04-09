import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.util.JiraHome

String body = "hello world!"
def jiraHome = ComponentAccessor.getComponent(JiraHome)
def file = new File(jiraHome.home, "file.txt") 
file.write body