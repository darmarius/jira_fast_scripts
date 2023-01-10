import com.atlassian.jira.component.ComponentAccessor

List<String> optionsList = getOptionsFromRemoteSource()

String fieldName = "Field"
def formField = getFieldByName(fieldName)
def customField = ComponentAccessor.customFieldManager.getCustomFieldObjectsByName(fieldName).first()

def fieldConfig = customField.getConfigurationSchemes().first().configs.values().first()
Map updatedOptionList = new HashMap()
for (String optionName:optionsList){
    def existsOption = ComponentAccessor.optionsManager.getOptions(fieldConfig).find{ it.value==optionName }
	if (!existsOption){
		ComponentAccessor.optionsManager.createOption(fieldConfig,0,1,optionName) 
        fieldConfig = customField.getConfigurationSchemes().first().configs.values().first()
        existsOption = ComponentAccessor.optionsManager.getOptions(fieldConfig).find{ it.value==name }
	}
    updatedOptionList.put(existsOption.getOptionId(),existsOption.getValue())
}	
cts.setFieldOptions(updatedOptionList)
cts.setFormValue(updatedOptionList.keySet().first())