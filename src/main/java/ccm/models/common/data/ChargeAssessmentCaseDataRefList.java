package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.dems.DemsCaseRef;
import ccm.models.system.dems.DemsCaseRefList;

public class ChargeAssessmentCaseDataRefList {
    private List<ChargeAssessmentCaseDataRef> case_list;

    public ChargeAssessmentCaseDataRefList() {
        case_list = new ArrayList<ChargeAssessmentCaseDataRef>();
    }

    public ChargeAssessmentCaseDataRefList(DemsCaseRefList demsCaseRefList) {
        this();

        for (DemsCaseRef demsCaseRef : demsCaseRefList.getList()) {
            ChargeAssessmentCaseDataRef caseRef = new ChargeAssessmentCaseDataRef(demsCaseRef);
            case_list.add(caseRef);
        }
    }

    public List<ChargeAssessmentCaseDataRef> getCase_list() {
        return case_list;
    }

    public void setCase_List(List<ChargeAssessmentCaseDataRef> case_list) {
        this.case_list = case_list;
    }
}
