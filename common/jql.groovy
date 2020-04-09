import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.web.bean.PagerFilter

def applicationUser = ComponentAccessor.getUserManager().getUserByName("Admin")
SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class)
String jql ="project = test"
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jql)
final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
def issues = results.getResults() // jira < 8.0.0
//def issues = results.getIssues() // jira > 8.0.0