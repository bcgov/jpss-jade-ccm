package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinCaseHyperlinkData;
import ccm.models.system.justin.JustinCaseHyperlinkDataList;


public class CaseHyperlinkDataList {
    private List<CaseHyperlinkData> caseHyperlink;

    public CaseHyperlinkDataList() {
        caseHyperlink = new ArrayList<CaseHyperlinkData>();
    }

    public CaseHyperlinkDataList(JustinCaseHyperlinkDataList casehyperlinkdata) {
        List<CaseHyperlinkData> chd = new ArrayList<CaseHyperlinkData>();
        if(casehyperlinkdata.getCase_hyperlinks() != null) {
            for (JustinCaseHyperlinkData jas : casehyperlinkdata.getCase_hyperlinks()) {
                CaseHyperlinkData bcas = new CaseHyperlinkData(jas);
                chd.add(bcas);
            }
    }
    setCaseHyperLinkList(chd);
}
       
    public List<CaseHyperlinkData> getCase_list() {
        return caseHyperlink;
    }

    public void setCaseHyperLinkList(List<CaseHyperlinkData> caseHyperlink) {
        this.caseHyperlink = caseHyperlink;
    }
}