package ccm.models.common.data;


import ccm.models.system.justin.JustinAuthUser;

public class AuthUser {
    private String part_id;
    private String jrs_role;

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
      setPart_id(ja.getPart_id());
      setJrs_role(ja.getJrs_role());

    }

    public AuthUser(String key, String role) {
      this.key = key;
      this.role = role;
    }
    public String getPart_id() {
      return part_id;
    }

    public void setPart_id(String part_id) {
      this.part_id = part_id;
    }

    public String getJrs_role() {
      return jrs_role;
    }

    public void setJrs_role(String jrs_role) {
      this.jrs_role = jrs_role;
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
  