package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ccm.models.common.CommonAuthUser;
import ccm.models.common.CommonAuthUsersList;

public class DemsAuthUsersList {
    private List<String> userKeys;

    public DemsAuthUsersList() {
    }

    public DemsAuthUsersList(CommonAuthUsersList b) {
      Iterator<CommonAuthUser> i = b.getAuth_users_list().iterator();

      List<String> l = new ArrayList<String>();
      
      while (i != null && i.hasNext()) {
        l.add(i.next().getPart_id());
      }

      setUserKeys(l);
    }

    public List<String> getUserKeys() {
      return userKeys;
    }

    public void setUserKeys(List<String> userKeys) {
      this.userKeys = userKeys;
    }
  }
  