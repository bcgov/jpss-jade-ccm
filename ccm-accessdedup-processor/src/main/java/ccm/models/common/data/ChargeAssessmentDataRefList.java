package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.dems.DemsCaseRef;
import ccm.models.system.dems.DemsCaseRefList;

public class ChargeAssessmentDataRefList {
    private List<ChargeAssessmentDataRef> case_list;

    public ChargeAssessmentDataRefList() {
        case_list = new ArrayList<ChargeAssessmentDataRef>();
    }

    public ChargeAssessmentDataRefList(DemsCaseRefList demsCaseRefList) {
        this();

        for (DemsCaseRef demsCaseRef : demsCaseRefList.getList()) {
            ChargeAssessmentDataRef caseRef = new ChargeAssessmentDataRef(demsCaseRef);
            case_list.add(caseRef);
        }
    }

    public List<ChargeAssessmentDataRef> getCase_list() {
        return case_list;
    }

    public void setCase_List(List<ChargeAssessmentDataRef> case_list) {
        this.case_list = case_list;
    }
}
