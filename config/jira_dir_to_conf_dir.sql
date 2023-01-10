/*stop confluence
check your internal directory id
there is 98305
purge the fingerprints in content from internal users*/

update notifications set CREATOR = null where CREATOR in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update notifications set LASTMODIFIER = null where LASTMODIFIER in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
delete from notifications where USERNAME in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update links set CREATOR = null where CREATOR in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update links set LASTMODIFIER = null where LASTMODIFIER in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
delete from likes where USERNAME in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update label set OWNER = null where OWNER in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update content_label set OWNER = null where OWNER in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update content set CREATOR = null where CREATOR in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
update content set LASTMODIFIER = null where LASTMODIFIER in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
delete from content where CONTENTTYPE = 'USERINFO' and USERNAME in 
	(select user_key from conf.user_mapping where lower_username in 
		(select lower_user_name from cwd_user where directory_id = 98305));
delete from user_mapping where lower_username in (select lower_user_name from cwd_user where directory_id = 98305));

/*purge internal groups data*/

delete from cwd_group_attribute where directory_id = 98305;
delete from cwd_membership where parent_id in (select id from cwd_group where directory_id = 98305);
delete from cwd_membership where child_group_id in (select id from cwd_group where directory_id = 98305);
delete from cwd_membership where child_user_id in (select id from cwd_user where directory_id = 98305);
delete from cwd_group where directory_id = 98305;

/*purge internal users data*/

delete from cwd_user_attribute;
delete from cwd_user where directory_id = 98305;

/*substitute external data to internal*/

update cwd_group_attribute set directory_id = 98305;
update cwd_group set directory_id = 98305;
update cwd_user set directory_id = 98305;

/*set password 'admin' to admin user*/

update cwd_user set credential = '8WEZjkCbLWysbcbZ5PRgMbdJgJOhkzRT3y1jxOqke2z1Zr79q8ypugFQEYaMoIZt' where id = 10000;

/*enable internal directory, disable external directory*/

update cwd_directory set active = 'T' where (id = 98305);
update cwd_directory set active = 'F' where (id <> 98305);

/*start confluence*/