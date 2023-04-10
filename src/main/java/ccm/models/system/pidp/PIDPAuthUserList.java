package ccm.models.system.pidp;

import java.util.List;

import ccm.models.common.data.AuthUser;

public class PIDPAuthUserList {
    private String key;
    private List<AuthUser> authUserList;

    public PIDPAuthUserList() {}
    
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    
    public List<AuthUser> getAuthUserList() {
        return authUserList;
    }
    public void setAuthUserList(List<AuthUser> authUserList) {
        this.authUserList = authUserList;
    }

    
}
