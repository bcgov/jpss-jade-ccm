package ccm.models.system.justin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ccm.models.common.data.CaseHyperlinkDataList;
import ccm.models.common.data.CaseHyperlinkData;

public class JustinCaseHyperlinkDataList {
    String message;
    //CaseHyperlinkDataList case_hyperlinks;
    private List<CaseHyperlinkData> case_hyperlinks_test = new ArrayList<CaseHyperlinkData>();
    private List<CaseHyperlinkData> case_hyperlinks;

    public JustinCaseHyperlinkDataList(CaseHyperlinkDataList caseHyperlinkData) {
        System.out.println("Inside JustinCaseHyperlinkDataList : "+caseHyperlinkData.getcase_hyperlinks().get(0));
        //case_hyperlinks_test.add(caseHyperlinkData);
        Iterator<CaseHyperlinkData> i = caseHyperlinkData.getcase_hyperlinks().iterator();
        while (i != null && i.hasNext()) {
            case_hyperlinks_test.add(i.next());
          }

        System.out.println("case_hyperlinks_test : "+ case_hyperlinks_test);
       //this.setCase_hyperlinks(caseHyperlinkData);
        this.setCase_hyperlinks(case_hyperlinks_test);
    }

    public void setCase_hyperlinks(List<CaseHyperlinkData> case_hyperlinks) {
        this.case_hyperlinks = case_hyperlinks;
    }
    public List<CaseHyperlinkData> getCase_hyperlinks() {
        return case_hyperlinks;
    }

    public JustinCaseHyperlinkDataList() {
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    /*public CaseHyperlinkDataList getCase_hyperlinks() {
        return case_hyperlinks;
    }
    public void setCase_hyperlinks(CaseHyperlinkDataList case_hyperlinks) {
        this.case_hyperlinks = case_hyperlinks;
    }*/

}