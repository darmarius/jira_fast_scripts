function getCurrentUserGroups(){
	var userGroups;
	AJS.$.ajax({
		url: "/rest/api/2/myself?expand=groups",
		type: 'get',
		dataType: 'json',
		async: false,
		success: function(data) {
			userGroups=data.groups.items	}
	});
	return userGroups;
}

function hideTimeTracking(){
	document.querySelector("#timetrackingmodule").hidden=true;
	document.querySelector("#collaboratorsmodule").hidden=true;
	document.querySelector("#worklog-tabpanel").hidden=true;
	AJS.$("#jeti-trigger").hide();
}

var groups = getCurrentUserGroups();
for (var i=0;i<groups.length;i++){
	if(groups[i].name=='jira-external'){
		hideTimeTracking();
	}
}



