package ccm.dems.useraccess.adapter.models.system.dems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ccm.dems.useraccess.adapter.models.common.data.AuthUser;
import ccm.dems.useraccess.adapter.models.common.data.AuthUserList;

public class DemsAuthUsersList {
    private List<String> userKeys;

    public DemsAuthUsersList() {
    }

    public DemsAuthUsersList(AuthUserList commonList) {
        Iterator<AuthUser> i = commonList.getAuth_user_list().iterator();

        List<String> demsList = new ArrayList<String>();

        while (i != null && i.hasNext()) {
            demsList.add(i.next().getPart_id());
        }

        setUserKeys(demsList);
    }

    public List<String> getUserKeys() {
        return userKeys;
    }

    public void setUserKeys(List<String> userKeys) {
        this.userKeys = userKeys;
    }
}
