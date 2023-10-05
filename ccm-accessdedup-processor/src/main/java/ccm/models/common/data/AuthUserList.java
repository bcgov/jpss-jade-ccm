package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinAuthUser;
import ccm.models.system.justin.JustinAuthUsersList;
import ccm.models.system.pidp.PIDPAuthUserList;

public class AuthUserList {
    private String rcc_id;
    private List<AuthUser> auth_user_list;

    public AuthUserList() {
      auth_user_list = new ArrayList<AuthUser>();
    }

    public AuthUserList(JustinAuthUsersList jal) {
      setRcc_id(jal.getRcc_id());

      List<AuthUser> l = new ArrayList<AuthUser>();
      if (jal != null) {
      for (JustinAuthUser ja : jal.getAuthuserslist()) {
        AuthUser ba = new AuthUser(ja);
        l.add(ba);
      }
     }

      setAuth_user_list(l);
    }

    public void addJustinAuthUserList(JustinAuthUsersList jal) {
      if (this.auth_user_list == null) {
        this.auth_user_list = new ArrayList<AuthUser>();
      }
      if (jal != null) {
      for (JustinAuthUser ja : jal.getAuthuserslist()) {
        AuthUser ba = new AuthUser(ja);
        this.auth_user_list.add(ba);
      }
      }
    }

    public void AddPdipAuthUserList(PIDPAuthUserList pal) {
      if (this.auth_user_list == null) {
        this.auth_user_list = new ArrayList<AuthUser>();
      }
      if (pal != null) {
      for (AuthUser ja : pal.getAuthUserList()) {
       
        this.auth_user_list.add(ja);
      }
      }
    }
    public AuthUserList(PIDPAuthUserList authList) {
      List<AuthUser> l = new ArrayList<AuthUser>();
      if (authList != null) {
      for(AuthUser user : authList.getAuthUserList()){
        l.add(user);
      }
     }
    }
  
    public String getRcc_id() {
      return this.rcc_id;
    }
  
    public void setRcc_id(String rcc_id) {
      this.rcc_id = rcc_id;
    }

    public List<AuthUser> getAuth_user_list() {
      return auth_user_list;
    }

    public void setAuth_user_list(List<AuthUser> auth_user_list) {
      this.auth_user_list = auth_user_list;
    }

    
  }
  