import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor

//get currentUser
ApplicationUser applicationUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
//set currentUser
ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.userManager.getUserByName('Admin'))