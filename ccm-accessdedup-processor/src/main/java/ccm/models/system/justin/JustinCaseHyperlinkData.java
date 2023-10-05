package ccm.models.system.justin;

import ccm.models.common.data.CaseHyperlinkData;

public class JustinCaseHyperlinkData {
    String message;
    String hyperlink;

    public JustinCaseHyperlinkData() {
    }

    public JustinCaseHyperlinkData(CaseHyperlinkData caseHyperlinkData) {
        this.setMessage(caseHyperlinkData.getMessage());
        this.setHyperlink(caseHyperlinkData.getHyperlink());
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getHyperlink() {
        return hyperlink;
    }
    public void setHyperlink(String hyperlink) {
        this.hyperlink = hyperlink;
    }
}