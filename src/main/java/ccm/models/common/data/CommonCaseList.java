package ccm.models.common.data;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinRccCaseList;
public class CommonCaseList {
    List<String> rcc_ids;

    public CommonCaseList() {
    }
    public CommonCaseList(JustinRccCaseList jdl) {
      List<String> rccList = new ArrayList<String>();

    if(jdl.getRcc_ids() != null) {
        rccList.addAll(jdl.getRcc_ids());
      }
    setRcc_ids(rccList);
    }

    public List<String> getRcc_ids() {
        return rcc_ids;
    }
    public void setRcc_ids(List<String> rcc_ids) {
        this.rcc_ids = rcc_ids;
    }
}