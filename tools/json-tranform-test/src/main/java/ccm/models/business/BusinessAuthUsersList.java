package ccm.models.business;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinAuthUser;
import ccm.models.system.justin.JustinAuthUsersList;

public class BusinessAuthUsersList {
    private String rcc_id;
    private List<BusinessAuthUser> auth_users_list;

    public BusinessAuthUsersList(JustinAuthUsersList jal) {
      setRcc_id(jal.getRcc_id());

      List<BusinessAuthUser> l = new ArrayList<BusinessAuthUser>();
      
      for (JustinAuthUser ja : jal.getAuthuserslist()) {
        BusinessAuthUser ba = new BusinessAuthUser(ja);
        l.add(ba);
      }

      setAuth_users_list(l);
    }
  
    public String getRcc_id() {
      return this.rcc_id;
    }
  
    public void setRcc_id(String rcc_id) {
      this.rcc_id = rcc_id;
    }

    public List<BusinessAuthUser> getAuth_users_list() {
      return auth_users_list;
    }

    public void setAuth_users_list(List<BusinessAuthUser> auth_users_list) {
      this.auth_users_list = auth_users_list;
    }

    
  }
  