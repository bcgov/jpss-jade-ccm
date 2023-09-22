package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;


public class CaseHyperlinkDataList {
    private List<CaseHyperlinkData> case_hyperlinks = new ArrayList<CaseHyperlinkData>();

    public CaseHyperlinkDataList() {
        case_hyperlinks = new ArrayList<CaseHyperlinkData>();
    }

    public CaseHyperlinkDataList(CaseHyperlinkData body) {
       // List<CaseHyperlinkData> chd = new ArrayList<CaseHyperlinkData>();
       case_hyperlinks.add(body);    
        setcase_hyperlinks(case_hyperlinks);
        System.out.println("data 1:"+case_hyperlinks);
    }
       
    public List<CaseHyperlinkData> getcase_hyperlinks() {
        return case_hyperlinks;
    }

    public void setcase_hyperlinks(List<CaseHyperlinkData> caseHyperlink) {
        this.case_hyperlinks = caseHyperlink;
    }
}