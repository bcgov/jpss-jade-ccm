package ccm.models.system.justin;

import java.util.List;

import ccm.models.common.data.CaseHyperlinkDataList;

public class JustinCaseHyperlinkDataList {
    String message;
    List<JustinCaseHyperlinkData> case_hyperlinks;

    public JustinCaseHyperlinkDataList(CaseHyperlinkDataList caseHyperlinkData) {
        this.setCase_hyperlinks(case_hyperlinks);
    }

    public JustinCaseHyperlinkDataList() {
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public List<JustinCaseHyperlinkData> getCase_hyperlinks() {
        return case_hyperlinks;
    }
    public void setCase_hyperlinks(List<JustinCaseHyperlinkData> case_hyperlinks) {
        this.case_hyperlinks = case_hyperlinks;
    }

}