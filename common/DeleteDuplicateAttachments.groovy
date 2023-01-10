import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.attachment.Attachment

String jqlQuery = "issue = AP-1177"
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey("jiraadmin")
SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class)
def attachmentManager = ComponentAccessor.getAttachmentManager()
List<Issue> issuesUpdated = new ArrayList<>()
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlQuery);
final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
List<Issue> issues = results.getResults()

for(Issue issue: issues){
  def attaches = attachmentManager.getAttachments(issue)
  if (attaches.size()>1){    
    for (def y=0;y<=attaches.size()-1;y++){
      for (def z=y+1;z<=attaches.size()-1;z++){  
        if (attaches[y].filesize == attaches[z].filesize && 
            attaches[y].mimetype == attaches[z].mimetype && 
            //attaches[y].filename == attaches[z].filename && 
            attaches[y] != attaches[z]){
          attachmentManager.deleteAttachment(attaches[z])
          issuesUpdated.add(issue)
        }        	
      }
    }
  }
}
issuesUpdated.unique()
return issuesUpdated*.key