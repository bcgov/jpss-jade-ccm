package ccm.models.common.data;


import ccm.models.system.justin.JustinAuthUser;

public class AuthUser {
    private String part_id;
    private String jrs_role;

    public AuthUser() {
    }

    public AuthUser(JustinAuthUser ja) {
      setPart_id(ja.getPart_id());
      setJrs_role(ja.getJrs_role());

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
  
    
  }
  