package helper

import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeValueBean
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.query.Query
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import org.apache.log4j.Level
import org.apache.log4j.Logger
import com.atlassian.jira.issue.IssueManager
import com.riadalabs.jira.plugins.insight.services.model.ObjectTypeAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.factory.ObjectAttributeBeanFactory
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeAttributeFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectTypeFacade

public class InsightHelper {
    
    @WithPlugin("com.riadalabs.jira.plugins.insight")
    @PluginModule
    private static IQLFacade iqlFacade
    @PluginModule
    private static ObjectFacade objectFacade
    @PluginModule
    private static ObjectTypeFacade objectTypeFacade
    @PluginModule
    private static ObjectAttributeBeanFactory objectAttributeBeanFactory
    @PluginModule
    private static ObjectTypeAttributeFacade objectTypeAttributeFacade

    private static UserManager userManager = ComponentAccessor.getUserManager()
    private static JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    private static SearchService searchService = ComponentAccessor.getComponent(SearchService)
    private static ApplicationUser admin = userManager.getUserByName("jiraadmin")
    private static Logger log = Logger.getLogger(this.class.name)
    private static IssueManager issueManager = ComponentAccessor.getIssueManager()

    public static ObjectAttributeValueBean getValueOfInsightProperty(ObjectBean insightObj,String propertyName){
        int objectId = insightObj.getId()
		def objectAttribute = objectFacade.loadObjectAttributeBean(objectId, propertyName)
        if(objectAttribute){
            def objectAttributeValues = objectAttribute.getObjectAttributeValueBeans()
            return objectAttributeValues[0]
        }else{
            return null;
        }
    }

    public static ObjectAttributeValueBean getValueOfInsightProperty(int objectId,String propertyName){
		def objectAttribute = objectFacade.loadObjectAttributeBean(objectId, propertyName)
        if(objectAttribute){
            def objectAttributeValues = objectAttribute.getObjectAttributeValueBeans()
            return objectAttributeValues[0]
        }else{
            return null;
        }
    }

    public static void clearValue(ObjectBean insightObj,String propertyName){
        def objectAttribute = objectFacade.loadObjectAttributeBean(insightObj.getId(), propertyName).createMutable()
        def value = objectAttribute.getObjectAttributeValueBeans()
        value.clear()
        objectFacade.deleteObjectAttributeBean(objectAttribute.id)
    }

    public static void clearValue(int objectId,String propertyName){
        def objectAttribute = objectFacade.loadObjectAttributeBean(objectId, propertyName).createMutable()
        def value = objectAttribute.getObjectAttributeValueBeans()
        value.clear()
        objectFacade.deleteObjectAttributeBean(objectAttribute.id)
    }

    public static List<ObjectBean> getObjectByIql(String iql){
        List<ObjectBean> objectCollection = iqlFacade.findObjects(iql)
        return objectCollection
    }

    public static Map lazyUpdate(ObjectBean object, Map<String,List> newAttributes) {
        log.setLevel(Level.DEBUG)
        log.debug("newAttributes: $newAttributes / class value is ${newAttributes.values().first().class}")
        int objectTypeId = object.getObjectTypeId()
        log.debug("objectTypeId: $objectTypeId")
        List<ObjectTypeAttributeBean> objectTypeAttributeBeans = objectTypeAttributeFacade.findObjectTypeAttributeBeans(objectTypeId)
        List objectTypeAttributeNames = objectTypeAttributeBeans.collect{it.getName().toLowerCase()}
        log.debug("objectTypeAttributeNames: $objectTypeAttributeNames")
        newAttributes.findAll {
            it.key.toLowerCase() in objectTypeAttributeNames
        }.collectEntries { attr, values ->
        ObjectTypeAttributeBean objectTypeAttributeBean = objectTypeAttributeBeans.find {
            it.getName().toLowerCase() == attr.toLowerCase()
        }
        def newObjectAttributeBean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(object, objectTypeAttributeBean, values.getAt(0) as String)
        def objectAttributeBean = objectFacade.loadObjectAttributeBean(object.getId(), objectTypeAttributeBean.getId());
        if (objectAttributeBean != null) {
            newObjectAttributeBean.setId(objectAttributeBean.getId());
        }
        try {
            objectAttributeBean = objectFacade.storeObjectAttributeBean(newObjectAttributeBean);
        } catch (Exception vie) {
            log.warn("Could not update object attribute due to validation exception:" + vie.getMessage());
        }
        [objectTypeAttributeBean.getName(),objectAttributeBean.getObjectAttributeValueBeans()*.getValue()]
        }
    }

    public static ObjectBean createUserInsightObject(Map<String,String> attList, int insightTypeId){
        log.setLevel(Level.DEBUG)
        def objectTypeCustomer = objectTypeFacade.loadObjectTypeBean(insightTypeId)
		def newObjectBean = objectTypeCustomer.createMutableObjectBean()
		def objectAttributeBeans = new ArrayList()

        int objTypeId = newObjectBean.getObjectTypeId()
   		attList.each{key, value ->
            if(value && value!="null"){
                log.warn key+" is !"+value+"!"
                ObjectTypeAttributeBean attObjTypeBean = objectTypeAttributeFacade.loadObjectTypeAttributeBean(objTypeId, key)  
                def bean = objectAttributeBeanFactory.createObjectAttributeBeanForObject(newObjectBean, attObjTypeBean, value)
                objectAttributeBeans.add(bean)
             }
        }

		newObjectBean.setObjectAttributeBeans(objectAttributeBeans)
   		
        try {
  		   return objectFacade.storeObjectBean(newObjectBean)
		} catch (Exception vie) {
		    log.warn("Could not create object due to validation exception:" + vie.getMessage())
		}      
    }

    public static String getId(String objectType, String name){
        String iql = """objectType = "${objectType}" AND Name = "${name}" """ // текст запроса в инсайт
        List<ObjectBean> insightReporterObject = iqlFacade.findObjects(iql) as List<ObjectBean> 
        String id
        if(insightReporterObject){
            ObjectBean object = insightReporterObject.first()
            id = objectFacade.loadObjectAttributeBean(object.id, "ID").objectAttributeValueBeans.first().value
        }
        return id
    }
}