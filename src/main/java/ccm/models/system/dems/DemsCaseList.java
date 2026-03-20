package ccm.models.system.dems;

import java.util.List;

public class DemsCaseList {

    List<String> case_ids;

    public DemsCaseList() {
    }

    public List<String> getCaseIds() {
        return case_ids;
    }
    public void setCaseIds(List<String> case_ids) {
        this.case_ids = case_ids;
    }
}