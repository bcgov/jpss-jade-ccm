package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;


public class CaseHyperlinkDataList {
    private List<CaseHyperlinkData> caseHyperlink;

    public CaseHyperlinkDataList() {
        caseHyperlink = new ArrayList<CaseHyperlinkData>();
    }

    public CaseHyperlinkDataList(CaseHyperlinkData body) {
        List<CaseHyperlinkData> chd = new ArrayList<CaseHyperlinkData>();
        chd.add(body);    
        setCaseHyperLinkList(chd);
    }
       
    public List<CaseHyperlinkData> getCase_list() {
        return caseHyperlink;
    }

    public void setCaseHyperLinkList(List<CaseHyperlinkData> caseHyperlink) {
        this.caseHyperlink = caseHyperlink;
    }
}