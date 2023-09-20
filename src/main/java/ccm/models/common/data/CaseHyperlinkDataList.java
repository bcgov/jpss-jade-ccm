package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;


public class CaseHyperlinkDataList {
    private List<CaseHyperlinkData> case_hyperlinks;

    public CaseHyperlinkDataList() {
        case_hyperlinks = new ArrayList<CaseHyperlinkData>();
    }

    public CaseHyperlinkDataList(CaseHyperlinkData body) {
        this();
        case_hyperlinks.add(body);
       /* List<CaseHyperlinkData> chd = new ArrayList<CaseHyperlinkData>();
        chd.add(body);    
        setcase_hyperlinks(chd);*/
        setcase_hyperlinks(case_hyperlinks);
        System.out.println(case_hyperlinks);
    }
       
    public List<CaseHyperlinkData> getcase_hyperlinks() {
        return case_hyperlinks;
    }

    public void setcase_hyperlinks(List<CaseHyperlinkData> caseHyperlink) {
        this.case_hyperlinks = caseHyperlink;
    }
}