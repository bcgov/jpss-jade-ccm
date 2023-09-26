package ccm.models.system.justin;

import ccm.models.common.data.CaseHyperlinkData;

public class JustinCaseHyperlinkData {
    String message;
    String hyperlink;
    String rcc_id;

    public JustinCaseHyperlinkData() {
    }

    public JustinCaseHyperlinkData(CaseHyperlinkData caseHyperlinkData) {
        this.setMessage(caseHyperlinkData.getMessage());
        this.setHyperlink(caseHyperlinkData.getHyperlink());
        this.setRcc_id(caseHyperlinkData.getRcc_id());
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
    public String getRcc_id() {
        return rcc_id;
    }
    public void setRcc_id(String rcc_id) {
        this.rcc_id = rcc_id;
    }
}