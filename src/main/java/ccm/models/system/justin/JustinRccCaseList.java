package ccm.models.system.justin;

import ccm.models.common.data.CommonCaseList;
import java.util.List;

public class JustinRccCaseList {

    List<String> rcc_ids;

    public JustinRccCaseList() {
    }

    public JustinRccCaseList(CommonCaseList commonCaseList) {
        this.setRcc_ids(commonCaseList.getRcc_ids());
    }

    public List<String> getRcc_ids() {
        return rcc_ids;
    }
    public void setRcc_ids(List<String> rcc_ids) {
        this.rcc_ids = rcc_ids;
    }
}