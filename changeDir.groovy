
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.crowd.embedded.api.CrowdDirectoryService;
import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.OperationType;
import com.atlassian.crowd.embedded.api.User;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.Level
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.manager.directory.DirectoryManager;
import com.atlassian.crowd.model.group.GroupTemplate;
import com.atlassian.crowd.model.group.GroupTemplateWithAttributes;
import com.atlassian.crowd.model.group.GroupWithAttributes;
import com.atlassian.crowd.model.user.UserTemplate;
import com.atlassian.crowd.model.user.UserWithAttributes;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

List<Map> directoriesJson = new ArrayList()
CrowdDirectoryService crowdDirectoryService = ComponentAccessor.getComponent(com.atlassian.crowd.embedded.api.CrowdDirectoryService) 
List<Directory> allDirectories = crowdDirectoryService.findAllDirectories();
for (Directory directory : allDirectories) {
  if (directory.isActive()) {
      Set<OperationType> allowedOperations = directory.getAllowedOperations();
      boolean copyUserOnLogin = false;
      String copyUserOnLoginString = (String)directory.getAttributes().get("crowd.delegated.directory.auto.create.user");
      if (copyUserOnLoginString != null)copyUserOnLogin = Boolean.parseBoolean(copyUserOnLoginString); 
      Map directoryJson = new HashMap();
      directoryJson.put("name", directory.getName());
      directoryJson.put("id", directory.getId());
      directoryJson.put("type", directory.getType().toString());
      directoryJson.put("create_user", Boolean.valueOf(allowedOperations.contains(OperationType.CREATE_USER)));
      directoryJson.put("delete_user", Boolean.valueOf(allowedOperations.contains(OperationType.DELETE_USER)));
      directoryJson.put("copy_user_on_login", Boolean.valueOf(copyUserOnLogin));
      directoriesJson.add(directoryJson);
    } 
  } 
log.error directoriesJson.toString()
ChangeDirectoryAction cda = new ChangeDirectoryAction(1,"COPY")
Collection<ApplicationUser> usersToMove = ComponentAccessor.userManager.getAllApplicationUsers().findAll { 
  it.emailAddress.contains("andda") 
  }.each{
    cda.execute(it)
  }
return usersToMove



public class ChangeDirectoryAction{
  private static final Logger log = Logger.getLogger(ChangeDirectoryAction.class);
  
  private String mode;
  
  private int targetDirectory;

  CrowdService crowdService = ComponentAccessor.getComponent(com.atlassian.crowd.embedded.api.CrowdService)
  DirectoryManager directoryManager = ComponentAccessor.getComponent(com.atlassian.crowd.manager.directory.DirectoryManager)
  
  public ChangeDirectoryAction(int targetDirectory,  String mode) {
    this.targetDirectory = targetDirectory;
    this.mode = mode
    this.log.setLevel(Level.DEBUG)
  }
  
  public String execute(ApplicationUser user) {
    String targetDirectoryName;
    List<String> groupsForUser = ComponentAccessor.getGroupManager().getGroupNamesForUser(user) as List<String>
    try {
      Directory targetDirectoryObject = this.directoryManager.findDirectoryById(this.targetDirectory);
      if (targetDirectoryObject != null) {
        targetDirectoryName = targetDirectoryObject.getName();
      } else {
        return "FAILED";
      } 
    } catch (Exception e) {
      log.warn(MessageFormat.format("Could not find target directory by id - {0}", new Object[] { Integer.valueOf(this.targetDirectory) }));
      return "FAILED"
    } 
    long sourceDirectory = user.getDirectoryId();
    String sourceDirectoryName = String.valueOf(sourceDirectory);
    try {
      sourceDirectoryName = this.directoryManager.findDirectoryById(sourceDirectory).getName();
    } catch (Exception e) {
      log.debug(MessageFormat.format("Unable to find source directory name for id {0}, will refer to by id instead", new Object[] { Long.valueOf(sourceDirectory) }));
    } 
    if (sourceDirectory == this.targetDirectory) {
      log.debug(MessageFormat.format("User - {0} is already in the target directory - {1}. Skipping...", new Object[] { user.getName(), targetDirectoryName }));
      return "SKIPPED"
    } 
    UserWithAttributes userWithAttributes = null;
    try {
      userWithAttributes = this.directoryManager.findUserWithAttributesByName(sourceDirectory, user.getName());
    } catch (Exception e) {
      log.warn(MessageFormat.format("Could not retrieve UserWithAttributes for user - {0} in directory - {1}", new Object[] { user.getName(), sourceDirectoryName }), (Throwable)e);
      return "FAILED"
    } 
    UserTemplate userTemplate = new UserTemplate(user as User);
    userTemplate.setDirectoryId(this.targetDirectory);
    userTemplate.setExternalId("");
    try {
      UserWithAttributes userWithAttributes1 = this.directoryManager.findUserWithAttributesByName(this.targetDirectory, user.getName());
      if (userWithAttributes1 != null) {
        log.debug(MessageFormat.format("A user with the name {0} already exists in target directory - {1}", 
          new Object[] { user.getName(), targetDirectoryName }));
        return "SKIPPED"
      } 
    } catch (Exception e) {
      log.warn(MessageFormat.format("Could not check target directory - {0} to see if user = {1} already exists", new Object[] { targetDirectoryName, user.getName() }));
      return "FAILED"
    } catch (Exception userNotFoundException) {}
    try {
      this.directoryManager.addUser(this.targetDirectory, userTemplate, new PasswordCredential(generateRandomPassword()));
    } catch (Exception e) {
      log.warn(MessageFormat.format("Could not add user - {0} to directory - {1}", new Object[] { userTemplate.getName(), targetDirectoryName }), e);
      return "FAILED"
    } 
    Map<String, Set<String>> attributeMap = new HashMap<>();
    for (String key : userWithAttributes.getKeys())
      attributeMap.put(key, userWithAttributes.getValues(key)); 
    try {
      this.directoryManager.storeUserAttributes(this.targetDirectory, user.getName(), attributeMap);
    } catch (Exception e) {
      log.warn(MessageFormat.format("Cannot store user attributes into new user - {0} in directory - {1}", new Object[] { user.getName(), targetDirectoryName }), e);
      return "FAILED"
    } 
    for (String group : groupsForUser) {
      try {
        try {
          this.directoryManager.findGroupByName(this.targetDirectory, group);
          this.directoryManager.addUserToGroup(this.targetDirectory, user.getName(), group);
        } catch (Exception e) {
          try {
            GroupWithAttributes groupWithAttributes = this.directoryManager.findGroupWithAttributesByName(sourceDirectory, group);
            GroupTemplateWithAttributes groupTemplate = new GroupTemplateWithAttributes(groupWithAttributes);
            groupTemplate.setDirectoryId(this.targetDirectory);
            this.directoryManager.addGroup(this.targetDirectory, (GroupTemplate)groupTemplate);
            this.directoryManager.storeGroupAttributes(this.targetDirectory, group, groupTemplate.getAttributes());
            this.directoryManager.addUserToGroup(this.targetDirectory, user.getName(), group);
          } catch (Exception e1) {
            log.warn(MessageFormat.format("Unable to copy group - {0} from directory - {1} to directory - {2}", new Object[] { group, sourceDirectoryName, targetDirectoryName }), (Throwable)e1);
            return "FAILED"
          } 
        } 
      } catch (Exception e) {
        log.warn(MessageFormat.format("Unable to copy group - {0} from directory - {1} to directory - {2}", new Object[] { group, sourceDirectoryName, targetDirectoryName }), e);
        return "FAILED"
      } 
    } 
    try {
      if (this.directoryManager.supportsExpireAllPasswords(user.getDirectoryId()))
        try {
          this.crowdService.setUserAttribute(user as User, "requiresPasswordChange", "true");
        } catch (Exception e) {
          log.info(MessageFormat.format("Could not set {0} attribute on user - {1} in directory - {2}", new Object[] { "requiresPasswordChange", user.getName(), targetDirectoryName }), (Throwable)e);
        }  
    } catch (Exception e) {
      log.debug(MessageFormat.format("Could not find if directory {0} supports requiring password changes", new Object[] { targetDirectoryName }), (Throwable)e);
    } 
    if (this.mode.equals("MOVE"))
      try {
        this.directoryManager.removeUser(sourceDirectory, user.getName());
      } catch (Exception e) {
        log.warn(MessageFormat.format("Could not delete user - {0} from source directory - {1} after moving to target directory {2}", new Object[] { user.getName(), sourceDirectoryName, targetDirectoryName }), e);
        return "FAILED"
      }  
    return "SUCCESS"
  }
  
  private String generateRandomPassword() {
    Random random = new SecureRandom();
    String lowerLetters = "abcdefghjkmnpqrstuvwxyz";
    String upperLetters = "ABCDEFGHJKMNPQRSTUVWXYZ";
    String numbers = "23456789";
    String specials = "!@#%^&*";
    StringBuilder password = new StringBuilder();
    int index = (int)(random.nextDouble() * lowerLetters.length());
    password.append(lowerLetters.substring(index, index + 1));
    index = (int)(random.nextDouble() * upperLetters.length());
    password.append(upperLetters.substring(index, index + 1));
    index = (int)(random.nextDouble() * numbers.length());
    password.append(numbers.substring(index, index + 1));
    index = (int)(random.nextDouble() * specials.length());
    password.append(specials.substring(index, index + 1));
    String chars = lowerLetters + upperLetters + numbers + specials;
    for (int i = 0; i < 15; i++) {
      index = (int)(random.nextDouble() * chars.length());
      password.append(chars.substring(index, index + 1));
    } 
    return password.toString();
  }
}
