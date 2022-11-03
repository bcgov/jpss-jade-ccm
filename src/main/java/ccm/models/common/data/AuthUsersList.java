package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinAuthUser;
import ccm.models.system.justin.JustinAuthUsersList;

public class AuthUsersList {
    private String rcc_id;
    private List<AuthUser> auth_users_list;

    public AuthUsersList() {
    }

    public AuthUsersList(JustinAuthUsersList jal) {
      setRcc_id(jal.getRcc_id());

      List<AuthUser> l = new ArrayList<AuthUser>();
      
      for (JustinAuthUser ja : jal.getAuthuserslist()) {
        AuthUser ba = new AuthUser(ja);
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

    public List<AuthUser> getAuth_users_list() {
      return auth_users_list;
    }

    public void setAuth_users_list(List<AuthUser> auth_users_list) {
      this.auth_users_list = auth_users_list;
    }

    
  }
  