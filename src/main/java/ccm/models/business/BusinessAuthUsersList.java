package ccm.models.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ccm.models.system.justin.JustinAuthUser;
import ccm.models.system.justin.JustinAuthUsersList;

public class BusinessAuthUsersList {
    private String rcc_id;
    private List<String> part_id_list;

    public BusinessAuthUsersList(JustinAuthUsersList jal) {
      setRcc_id(jal.getRcc_id());
      Iterator<JustinAuthUser> i = jal.getAuthuserslist().iterator();

      List<String> l = new ArrayList<String>();
      
      while (i != null && i.hasNext()) {
        part_id_list.add(i.next().getPart_id());
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
  