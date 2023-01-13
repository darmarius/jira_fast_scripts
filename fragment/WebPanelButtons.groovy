package Fragments

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.customfields.manager.OptionsManager
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.user.ApplicationUser
import org.apache.log4j.Level
import org.apache.log4j.Logger
import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import java.sql.Driver
import java.sql.Connection
import java.sql.SQLException

/*
    Description:
    Script Fragment
    Note: Renders web-section with buttons to create issues
    Location: atl.jira.view.issue.right.context
    Weight: 1
*/

return writer.write(new ButtonSection().run((MutableIssue) context.issue))

class ButtonSection {

    Logger log = Logger.getLogger(this.class.name)

    CustomFieldManager customFieldManager = ComponentAccessor.getComponent(CustomFieldManager)
    IssueLinkManager issueLinkManager = ComponentAccessor.getComponent(IssueLinkManager)
    IssueManager issueManager = ComponentAccessor.getComponent(IssueManager)
    GroupManager groupManager = ComponentAccessor.getComponent(GroupManager)
    OptionsManager optionsManager = ComponentAccessor.getComponent(OptionsManager)

    String jiraUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
    StringWriter writer = new StringWriter()
    MarkupBuilder xml = new MarkupBuilder(writer)

    Boolean havingLinkedIssues = false
    List<String> keyLinkedIssues = new ArrayList()
    List<String>  statusLinkedIssues = new ArrayList()

    ApplicationUser curUser
    MutableIssue curIssue

    String run(MutableIssue issue) {
        log.setLevel(Level.DEBUG)
        long scriptStartTime = System.currentTimeMillis()
        this.curIssue = issue
        log.debug("${curIssue.getKey()}: [Run] script started.")

        curUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

        getLinkedIssues()

        xml.style(type: "text/css",xmlStyle)

        if (havingLinkedIssues) {
            xml.h3(id: "created-issues-header", "Created Issues:")
            xml.span(id: "created-issues") {
                xml.ul(style: "list-style-type: none;")
                for(int i = 0; i<keyLinkedIssues.size();i++){
                    xml.li() {
                        xml.ul(class: "created-issueIssue") {
                            xml.li() { 
                                xml.a(href: "${jiraUrl}/browse/${keyLinkedIssues[i]}", "${keyLinkedIssues[i]} (Type)") 
                            }
                            xml.li() { 
                                xml.span(id: "lozenge-Issuee"+i.toString(), 
                                class: "aui-lozenge aui-lozenge-subtle ${getStatusLozengeColor(statusLinkedIssues[i])}", "${statusLinkedIssues[i]}") 
                            }
                        }
                    }
                }
            }
        }
        if (havingLinkedIssues) {
            xml.h3(id: "Issue-creation-header", "Create Issues:")
            xml.span(id: "Issue-creation-buttons") {
                xml.ul(style: "list-style-type: none;")
                if (isAllowedToCreateIssues()) {
                    /*Simple button
                    
                    xml.li(class: "ul-for-button") {
                        button(class: "aui-button",id: "button-create-Issue",onClick: """
                                AJS.\$('#button-create-Issue').spin();
                                document.getElementById('button-create-Issue').setAttribute('aria-disabled', true);
                                ${createIssueRequest()}
                                AJS.\$('#button-create-Issue').spinStop()""", "Create Issue")
                    }*/
                    xml.li(class: "ul-for-button") {
                        button(id: "dialog-issue-show-button", class: "aui-button", onClick: "AJS.dialog2('#create-issue-dialog').show();", "Create Issue")
                    }
                    //dialog
                    xml.section(role: "dialog", id: "create-issue-dialog", class: "aui-layer aui-dialog2 aui-dialog2-small", "aria-hidden": "true") {
                        header(class: "aui-dialog2-header") {
                            h2(class: "aui-dialog2-header-main", "Create Issue")
                            a(class: "aui-dialog2-header-close") {
                                span(class: "aui-icon aui-icon-small aui-iconfont-close-dialog", "Close")
                            }
                        }
                        div(class: "aui-dialog2-content") {
                            form (class:"aui"){
                                div(class: "field-group") {//Team
                                    label(for: "team-dropdown", "Team")
                                    select(class: "multi-select",size:"4", id: "team-dropdown", name: "team-dropdown", multiple:"multiple") {
                                        getOptions("Team").each { optionValue ->
                                            option("${optionValue}")
                                        }
                                    }
                                }
                                div(class: "field-group") {//Products
                                    label(for: "prod-dropdown", "Products")
                                    select(class: "select", id: "prod-dropdown", name: "prod-dropdown") {
                                        getProducts().each { optionValue ->
                                            if(optionValue == "HERO") option(selected:"selected","${optionValue}")
                                            else option("${optionValue}")
                                        }
                                    }
                                }/*
                                div(class: "field-group") {    //Description
                                    label(for: "descript-field", "Description")
                                    textarea(class: "textarea", id: "descript-field", name: "descript-field",curIssue.summary){
                                        curIssue.summary
                                    }
                                }*/
                            }

                        }
                        footer(class: "aui-dialog2-footer") {
                            div(class: "aui-dialog2-footer-actions") {
                                button(class: "aui-button aui-button-primary", id: "button-create-issue", onClick: """
                                AJS.\$('#button-create-issue').spin(); 
                                document.getElementById('button-create-issue').setAttribute('aria-disabled', true); 
                                AJS.dialog2('#create-issue-dialog').hide();
                                ${createIssueRequest()}
                                AJS.\$('#button-create-issue').spinStop()""", "Create")
                            }
                        }
                    }
                }
            }
        }

        long scriptWorkTime = System.currentTimeMillis() - scriptStartTime
        log.debug("${curIssue.getKey()}: [Run] script work time: ${scriptWorkTime} ms.")
        return writer
    }

    void getLinkedIssues() {
        issueLinkManager.getOutwardLinks(curIssue.getId()).each { outLink ->
            MutableIssue linkedIssue = issueManager.getIssueObject(outLink.getDestinationId())
            if (linkedIssue.getProjectObject().getKey() == "KEY") {
                havingLinkedIssues = true
                keyLinkedIssues.add(linkedIssue.getKey())
                statusLinkedIssues.add(linkedIssue.getStatus().getName())
            }
        }
    }

    static String getStatusLozengeColor(String statusName) {
        String lozengeColor = ""
        switch (statusName) {
            case "Develop":
            case "Approving":
            case "Reworking":
                lozengeColor = "aui-lozenge-current"
                break
            case "Approved":
            case "Done":
            case "Canceled":
                lozengeColor = "aui-lozenge-success"
                break
            default:
                break
        }
        return lozengeColor
    }

    String createIssueRequest() {

        String request = """

        function sleep(milliseconds) {
            const date = Date.now();
            let currentDate = null;
            do {
                currentDate = Date.now();
            } while (currentDate - date < milliseconds);
        }
		
        var team;
		var optionsTeam = document.querySelector("#team-dropdown").selectedOptions;
        if(optionsTeam){
            for(var i=0;i<optionsTeam.length;i++){
                team += optionsTeam.item(i).value + ',';
            }
        }
        var product = document.querySelector("#prod-dropdown").value;
        
        
        jQuery(function(\$) {
        	\$.ajax("${jiraUrl}/rest/scriptrunner/latest/custom/createIssueFromRest", {
    			type: 'POST',
                headers: {
                    "X-Atlassian-Token":"no-check"
                }, 
                contentType: "application/json; charset=utf-8",
    			dataType: "json",
            	async: false,
    			data: JSON.stringify({ 
                	"curIssue": "${curIssue.getKey()}",
                	"curUser": "${curUser.getUsername()}",
                    "issueForTeam": team,
                    "product": product
                }) 
  			}
  		)
        });
        sleep(7000);
        document.location.reload(true);
        """

        return request
    }

    Boolean isAllowedToCreateIssues() {
        Boolean isStatusAllowed = curIssue.getStatus().getName() in ["Review", "Resolved", "Done", "Corrected"]
        List<String> userGroups = groupManager.getGroupNamesForUser(curUser) as List<String>
        Boolean isUserInAllowedGroup = userGroups.contains("jira-administrators") || userGroups.contains("jira-sys-admin")

        return isStatusAllowed || isUserInAllowedGroup
    }

    List<String> getOptions(String cfName) {
        List<String> optionsList = []
        CustomField cf = customFieldManager.getCustomFieldObjectsByName(cfName).iterator().next()
        FieldConfig fieldConfig = cf.getRelevantConfig(curIssue)
        Options options = optionsManager.getOptions(fieldConfig)
        options.each { option ->
            if(!option.getDisabled())
                optionsList.add(option.getValue())
        }
        return optionsList
    }

    String xmlStyle = """
                    
                    .created-issues {
                        -moz-column-count: 2;
                        -webkit-column-count: 2;
                        column-count: 2;
                        list-style-type: none;
                        justify-content: space-between;
                        padding: 5px;
                    }
                    
                    #created-Issues-header {
                        margin: 0px 0px 5px 0px;
                        margin-block-start: 7px; 
                        font-family: Arial,sans-serif;
                        font-size:14px;                        
                    }
                    
                    .ul-for-button{
                        margin-left: -40px;
    					margin-bottom: 10px;
                    }
                    """
}