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

    public DemsAuthUsersList(CommonAuthUsersList commonList) {
      Iterator<CommonAuthUser> i = commonList.getAuth_users_list().iterator();

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
  