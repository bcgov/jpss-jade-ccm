package ccm.models.common;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinAuthUser;
import ccm.models.system.justin.JustinAuthUsersList;

public class CommonAuthUsersList {
    private String rcc_id;
    private List<CommonAuthUser> auth_users_list;

    public CommonAuthUsersList() {
    }

    public CommonAuthUsersList(JustinAuthUsersList jal) {
      setRcc_id(jal.getRcc_id());

      List<CommonAuthUser> l = new ArrayList<CommonAuthUser>();
      
      for (JustinAuthUser ja : jal.getAuthuserslist()) {
        CommonAuthUser ba = new CommonAuthUser(ja);
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

    public List<CommonAuthUser> getAuth_users_list() {
      return auth_users_list;
    }

    public void setAuth_users_list(List<CommonAuthUser> auth_users_list) {
      this.auth_users_list = auth_users_list;
    }

    
  }
  