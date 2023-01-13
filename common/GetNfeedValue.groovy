import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.valiantys.nfeed.api.IFieldValueService
import com.valiantys.nfeed.api.IFieldDisplayService


@WithPlugin("com.valiantys.jira.plugins.SQLFeed")
@PluginModule
IFieldValueService fieldValueService
@PluginModule
IFieldDisplayService fieldDisplayService


CustomField nfeedCustomField = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName("CF name").first()
def storedValue = fieldValueService.getFieldValues(issue.key, nfeedCustomField.getId())
String displayedValue = fieldDisplayService.getDisplayResult(issue.key, nfeedCustomField?.id).getDisplay()

//set values
fieldValueService.setFieldValues(issue.key, nfeedCustomField.id, storedValue)