package ccm.models.common.data;

import java.util.List;
import java.util.ArrayList;

import ccm.models.system.justin.JustinRccCaseList;


public class CommonCaseList {
    List<String> keys;

    public CommonCaseList() {
    }
    public CommonCaseList(JustinRccCaseList jdl) {
      List<String> rccList = new ArrayList<String>();

    if(jdl.getRcc_ids() != null) {
        rccList.addAll(jdl.getRcc_ids());
      }
      setKeys(rccList);
    }

    public List<String> getKeys() {
        return keys;
    }
    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
}