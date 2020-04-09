import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor

def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"))
def objectAttributeBeanFactory = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory"))
def ObjectTypeAttributeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade"))
def objectTypeFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor().getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade"))
ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.userManager.getUserByName('admin'))

MutableIssue issue = ComponentAccessor.issueManager.getIssueObject("TEST-1")
//fields from issue
def plainCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Plain").first()
def insightObjectCF = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("Insight Object").first()
//values from issue
String plainValue = plainCF.getValue(issue)
def insightObjectValue = insightObjectCF.getValue(issue)
//creating new insight object
int objectTypeId = 1
def objectType = objectTypeFacade.loadObjectTypeBean(objectTypeId)
def newObject= objectType.createMutableObjectBean()
def objectAttributeBeans = new ArrayList()
//insight object fields
def plainBean = ObjectTypeAttributeFacade.loadObjectTypeAttributeBean(123)
def insightObjectBean = ObjectTypeAttributeFacade.loadObjectTypeAttributeBean(321)
//setting attribute values
if (plainValue)
	objectAttributeBeans.add(objectAttributeBeanFactory.createObjectAttributeBeanForObject(newObject, plainBean, plainValue.toString()))
if (insightObjectValue)
	objectAttributeBeans.add(objectAttributeBeanFactory.createObjectAttributeBeanForObject(newObject, insightObjectBean, insightObjectValue.first().getName()))
//saving new company
newObject.setObjectAttributeBeans(objectAttributeBeans)
newObject = objectFacade.storeObjectBean(newObject)