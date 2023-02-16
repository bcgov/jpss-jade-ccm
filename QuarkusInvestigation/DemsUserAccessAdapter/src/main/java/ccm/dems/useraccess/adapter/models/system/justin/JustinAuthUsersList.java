package ccm.dems.useraccess.adapter.models.system.justin;

import java.util.List;

public class JustinAuthUsersList {
    private String rcc_id;
    private List<JustinAuthUser> authuserslist;

    public String getRcc_id() {
        return this.rcc_id;
    }

    public void setRcc_id(String rcc_id) {
        this.rcc_id = rcc_id;
    }

    public List<JustinAuthUser> getAuthuserslist() {
        return this.authuserslist;
    }

    public void setAuthuserslist(List<JustinAuthUser> authuserslist) {
        this.authuserslist = authuserslist;
    }
}
