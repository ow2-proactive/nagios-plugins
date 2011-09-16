import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Enumeration;

class MySecurityPolicy extends Policy {
 
    private PermissionCollection perms = null;
 
    //lazy initialization class holder
    private static class SecurityPolicyHolder {
    private static final MySecurityPolicy policy = new MySecurityPolicy();
    }
 
    private MySecurityPolicy() {
    perms = new Permissions();
    }
 
    public static MySecurityPolicy getPolicy() {
    return SecurityPolicyHolder.policy;
    }
 
    public PermissionCollection getPermissions(CodeSource codesource) {
        return perms;
    }
 
    //invoked when new Permission is added to the current security Policy
    public boolean implies(ProtectionDomain domain, Permission permission)  {
	 
	    //get permission collection from the domain
	    PermissionCollection domainPermissions = domain.getPermissions();
	 
	    //get enumeration of permission elements
	    Enumeration<Permission> permissions = domainPermissions.elements();
	 
	    
	 
	    //Checks to see if the specified permission is
	    //implied (subset of) by the collection of
	    //Permission objects held in this PermissionCollection
	    if (!domainPermissions.implies(permission)) {
	 
	        //permission collection in the domain is read-only,
	        //Exception will be thrown if Permission object
	        //is added to read-only collection
	        if (domainPermissions.isReadOnly()) {
	 
	            //Because collection is read-only,
	            //add Permission objects to the local
	            //permission collection instead
	            while(permissions.hasMoreElements()) {
	            	Permission p = permissions.nextElement();
	                if (!perms.implies(p)) {
	                   perms.add(p);
	                }
	            }
	 
	            //assign local permission collection as a
	            //domain permission collection
	            domainPermissions = perms;
	        } else {
	            //if domain permission collection is not read only,
	            //just add new permission to it
	          domainPermissions.add(permission);
	        }
	 
	        //check if now domain has the new Permission
	        return domainPermissions.implies(permission);
	    }
	 
        return false;
    }
 
    public void addPermission(Permission permission)  {
        perms.add(permission);
    }
        //you can write your own implementation
    //of refresh method
        @Override
    public void refresh() {
         Policy.setPolicy(this);
    }
}
