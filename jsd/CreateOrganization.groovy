import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.TransitionOptions.Builder
import com.atlassian.fugue.Option
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.servicedesk.api.ServiceDeskManager
import com.atlassian.servicedesk.api.organization.OrganizationService
import com.atlassian.servicedesk.api.organization.OrganizationsQuery
import com.atlassian.servicedesk.api.util.paging.LimitedPagedRequest
import com.atlassian.servicedesk.api.util.paging.LimitedPagedRequestImpl
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.servicedesk.api.ServiceDeskManager
import com.atlassian.servicedesk.api.organization.CustomerOrganization
import com.atlassian.servicedesk.api.organization.OrganizationService
import com.atlassian.servicedesk.api.organization.CreateOrganizationParameters
@WithPlugin("com.atlassian.servicedesk")

@PluginModule
ServiceDeskManager serviceDeskManager

@PluginModule
OrganizationService organizationService

def issue = ComponentAccessor.issueManager.getIssueObject("IS-38")
def users = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Пользователи").first()

def serviceDeskProject = serviceDeskManager.getServiceDeskForProject(issue.projectObject)

def nameOrg = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Полное название").first()
def cf = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("Организации").first()
def organizationsToAdd = getOrganizations()
return createOrganization(nameOrg.getValue(issue).toString(), serviceDeskProject.getId())

cf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(cf), organizationsToAdd), new DefaultIssueChangeHolder())

def createOrganization(String name, int projectId) {
    def adminUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser
    def organisationService = ComponentAccessor.getOSGiComponentInstanceOfType(OrganizationService)
    def orgBuilder = organisationService.newCreateBuilder().name(name).build()
    def newOrg = organisationService.createOrganization(adminUser, orgBuilder)
    def orgUpdaterBuilder = organisationService.newOrganizationServiceDeskUpdateParametersBuilder().organization(newOrg).serviceDeskId(projectId)
	def orgUpdater = orgUpdaterBuilder.build()
    def updatedsr = organisationService.addOrganizationToServiceDesk(adminUser,orgUpdater)
}
List<CustomerOrganization> getOrganizations() {
    def adminUser = ComponentAccessor.getJiraAuthenticationContext().loggedInUser
    def organisationService = ComponentAccessor.getOSGiComponentInstanceOfType(OrganizationService)
    def organizationQuery = organisationService.newOrganizationsQueryBuilder().build()
    def organization = organisationService.getOrganizations(adminUser, organizationQuery)
    organization.getResults()
}
