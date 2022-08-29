package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ccm.models.business.BusinessAuthUser;
import ccm.models.business.BusinessAuthUsersList;

public class DemsAuthUsersList {
    private String rcc_id;
    private List<String> part_id_list;

    public DemsAuthUsersList(BusinessAuthUsersList b) {
      setRcc_id(b.getRcc_id());
      Iterator<BusinessAuthUser> i = b.getAuth_users_list().iterator();

      List<String> l = new ArrayList<String>();
      
      while (i != null && i.hasNext()) {
        l.add(i.next().getPart_id());
      }

      setPart_id_list(l);
    }
  
    public String getRcc_id() {
      return this.rcc_id;
    }
  
    public void setRcc_id(String rcc_id) {
      this.rcc_id = rcc_id;
    }

    public List<String> getPart_id_list() {
      return part_id_list;
    }

    public void setPart_id_list(List<String> part_id_list) {
      this.part_id_list = part_id_list;
    }

    
  }
  