package ccm.dems.useraccess.adapter.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.dems.useraccess.adapter.models.system.justin.JustinAuthUser;
import ccm.dems.useraccess.adapter.models.system.justin.JustinAuthUsersList;

public class AuthUserList {
    private String rcc_id;
    private List<AuthUser> auth_user_list;

    public AuthUserList() {
    }

    public AuthUserList(JustinAuthUsersList jal) {
        setRcc_id(jal.getRcc_id());

        List<AuthUser> l = new ArrayList<AuthUser>();

        for (JustinAuthUser ja : jal.getAuthuserslist()) {
            AuthUser ba = new AuthUser(ja);
            l.add(ba);
        }

        setAuth_user_list(l);
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
