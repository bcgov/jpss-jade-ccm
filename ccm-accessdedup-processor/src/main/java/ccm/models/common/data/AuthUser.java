package ccm.models.common.data;


import ccm.models.system.justin.JustinAuthUser;

public class AuthUser {
   
    private String key;
    private String role;

    public static enum RoleTypes {
    PIDP_SUBMITTING_AGENCY 
    };
    
    public AuthUser() {
    }

    public AuthUser(JustinAuthUser ja) {

      setKey(ja.getPart_id());
      setRole(ja.getJrs_role());
     

    }

    public AuthUser(String key, String role) {
      this.key = key;
      this.role = role;
    }
   
  
    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }
    
    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }
  }
  