import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.project.Project
import com.atlassian.jira.security.PermissionManager
import com.atlassian.jira.security.plugin.ProjectPermissionKey


String projectKey = curIssue.getProjectObject().getKey()

boolean hasSDAgentRole(String projectKey, ApplicationUser loggedUser) {
    PermissionManager permissionManager = ComponentAccessor.getPermissionManager()
    boolean checker = false
    ProjectPermissionKey permissionKey = permissionManager.getAllProjectPermissions().find{it.getNameI18nKey() == "sd.project.permission.agent.key"}.getProjectPermissionKey()
    List<Project> listProjectPermissions = permissionManager.getProjects(permissionKey,loggedUser) as List
    if (listProjectPermissions) {
        logger.debug "${curIssue.key}: ${loggedUser} has agent role in ${listProjectPermissions*.getKey()} projects"
        if (listProjectPermissions.find{it.getKey() == projectKey}) {
            checker = true
            logger.debug "${curIssue.key}: ${loggedUser} has agent role in current project - ${projectKey}"
        }
        else logger.debug "${curIssue.key}: ${loggedUser} has not agent role in current project - ${projectKey}"
    }
    else logger.debug "${curIssue.key}: ${loggedUser} has no agent role anywhere"
    return checker
}