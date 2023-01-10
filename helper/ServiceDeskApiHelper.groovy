package helper

import com.atlassian.servicedesk.api.requesttype.RequestTypeService
import com.atlassian.servicedesk.api.request.type.CustomerRequestTypeService
import com.atlassian.servicedesk.api.user.UserFactory
import com.atlassian.servicedesk.api.util.paging.SimplePagedRequest
import com.atlassian.servicedesk.api.util.paging.PagedRequest
import com.atlassian.servicedesk.api.util.paging.PagedResponse
import com.atlassian.servicedesk.api.requesttype.RequestTypeQuery
import com.atlassian.servicedesk.api.requesttype.RequestType
import com.atlassian.servicedesk.api.user.UncheckedUser
import com.atlassian.servicedesk.api.request.type.CustomerRequestType
import com.atlassian.servicedesk.api.request.type.CustomerRequestTypeQuery
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.atlassian.servicedesk.api.approval.ApprovalService
import com.atlassian.servicedesk.api.approval.ApprovalQuery.Builder
import com.atlassian.servicedesk.api.approval.ApprovalQuery
import com.atlassian.servicedesk.api.approval.Approval
import com.atlassian.servicedesk.api.ServiceDeskManager
import com.atlassian.jira.project.Project
import java.net.URLDecoder;
import com.atlassian.servicedesk.api.feedback.RequestFeedbackDetailsService
import com.atlassian.servicedesk.api.feedback.RequestFeedbackDetails
import com.atlassian.servicedesk.api.feedback.RequestFeedbackToken

public class ServiceDeskApiHelper{
    @WithPlugin("com.atlassian.servicedesk")
    @PluginModule
    RequestTypeService requestTypeService
    @PluginModule
    CustomerRequestTypeService customerRequestTypeService
    @PluginModule
    ApprovalService approvalService
    @PluginModule
    ServiceDeskManager serviceDeskManager
    @PluginModule
    RequestFeedbackDetailsService requestFeedbackDetailsService

    UserManager userManager = ComponentAccessor.getComponent(UserManager)
    CustomFieldManager customFieldManager = ComponentAccessor.getComponent(CustomFieldManager)
    String baseurl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")

    ApplicationUser adminUser = userManager.getUserByName( "jiraadmin" )

    String getCustomerRequestTypeName(MutableIssue issue){
        CustomField cfReqType = customFieldManager.getCustomFieldObjectsByName("Customer Request Type").iterator().next()
        RequestTypeQuery requestTypeQuery = requestTypeService.newQueryBuilder().issue(issue.id).build()
        PagedResponse<RequestType> requestTypes = requestTypeService.getRequestTypes(adminUser, requestTypeQuery)
        if (requestTypes.results.size()==1) {
            return requestTypes.results.first().getName() //Получаем название реквеста
        } else {
            return null
        }
    }
    CustomerRequestType getCustomerRequestTypeID(String name){
        RequestType newRequestType
        //Ищем Request Type с нужным именем
        for(int limitRequest = 0;limitRequest <301;limitRequest += 100){
            PagedRequest pagedRequest  = new SimplePagedRequest(limitRequest,limitRequest+100)
            RequestTypeQuery requestTypeQuery = requestTypeService.newQueryBuilder().pagedRequest(pagedRequest).build()
            PagedResponse<RequestType> requestTypes = requestTypeService.getRequestTypes(adminUser, requestTypeQuery)
            if(requestTypes.results.find{it.getName()==name}){
                newRequestType = requestTypes.results.find{it.getName()==name}
                break
            }
        }
        if(!newRequestType) return null
        CustomerRequestTypeQuery customerRequestTypeQuery = customerRequestTypeService.newQueryBuilder().requestType(newRequestType).build()
        UserFactory userFactory = ComponentAccessor.getOSGiComponentInstanceOfType(UserFactory)
        UncheckedUser uncheckedUser = userFactory.getUncheckedUser()
        //получаем id найденного customer request type для записи в кастомное поле Customer Request Type
        CustomerRequestType customerRequestType = customerRequestTypeService.getCustomerRequestType(uncheckedUser, customerRequestTypeQuery)
    }
    List<ApplicationUser> getPendingApprovers(MutableIssue issue){
        ApprovalQuery approvalQuery = approvalService.newQueryBuilder().issue(issue.id).build()
        PagedResponse<Approval> approvals = approvalService.getApprovals(adminUser,approvalQuery)
        return approvalService.getApprovers(adminUser, approvals.last()).findAll{!it.getApproverDecision().isPresent()}.collect{it.getApproverUser()}
    }
    String getPortalURL(MutableIssue issue){
        int portalId = serviceDeskManager.getServiceDeskForProject(issue.getProjectObject()).getId()
        return "${baseurl}/servicedesk/customer/portal/${portalId}/${issue.key}"
    }
    Map<String,String> getParamsFromURL(String url){
        Map<String,String> paramsFromURL = new HashMap()
        try{
            String[] queryParams = url.substring(url.indexOf("?")+1).split('&') // safe operator for urls without query params
            paramsFromURL = queryParams.collectEntries { paramz -> paramz.split('=').collect { URLDecoder.decode(it) }}
        } catch (Exception ex){
            //do nothing
        } finally{
            return paramsFromURL
        }
    }
    String getRequestFeedbackUrl(MutableIssue issue){
        //Получаем ссылку на оценку тикета
        RequestFeedbackDetails requestFeedbackDetails = requestFeedbackDetailsService.getRequestFeedbackDetails(issue.reporter, issue)
        return requestFeedbackDetails.getRequestFeedbackToken().getCustomerPortalLink().toString()
    }
}