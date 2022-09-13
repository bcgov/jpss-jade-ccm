package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ccm.models.business.BusinessAuthUser;
import ccm.models.business.BusinessAuthUsersList;

public class DemsAuthUsersList {
    private List<String> userKeys;

    public DemsAuthUsersList() {
    }

    public DemsAuthUsersList(BusinessAuthUsersList b) {
      Iterator<BusinessAuthUser> i = b.getAuth_users_list().iterator();

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
  