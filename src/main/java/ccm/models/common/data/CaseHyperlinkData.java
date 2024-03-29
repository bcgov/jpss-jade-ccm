package ccm.models.common.data;

import ccm.models.system.justin.JustinCaseHyperlinkData;

public class CaseHyperlinkData {
    String message;
    String hyperlink;
    String rcc_id;

    public CaseHyperlinkData() {
        
    }
    public CaseHyperlinkData(JustinCaseHyperlinkData jaf) {
        setMessage(jaf.getMessage());
        setHyperlink(jaf.getHyperlink());
        setRcc_id(jaf.getRcc_id());
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