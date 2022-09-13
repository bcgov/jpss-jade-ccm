package ccm.models.business;


import ccm.models.system.justin.JustinAuthUser;

public class BusinessAuthUser {
    private String part_id;
    private String crown_agency;
    private String user_name;

    public BusinessAuthUser() {
    }

    public BusinessAuthUser(JustinAuthUser ja) {
      setPart_id(ja.getPart_id());
      setCrown_agency(ja.getCrown_agency());
      setUser_name(ja.getUser_name());

    }

    public String getPart_id() {
      return part_id;
    }

    public void setPart_id(String part_id) {
      this.part_id = part_id;
    }

    public String getCrown_agency() {
      return crown_agency;
    }

    public void setCrown_agency(String crown_agency) {
      this.crown_agency = crown_agency;
    }

    public String getUser_name() {
      return user_name;
    }

    public void setUser_name(String user_name) {
      this.user_name = user_name;
    }
  
    
  }
  