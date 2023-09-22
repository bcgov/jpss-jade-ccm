package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinCaseHyperlinkData;

public class CaseHyperlinkData {
    String message;
    String hyperlink;
    String rcc_id;
    private List<CaseHyperlinkData> case_hyperlink_data;

    public CaseHyperlinkData() {
        
    }
    public CaseHyperlinkData(JustinCaseHyperlinkData jaf) {
        case_hyperlink_data = new ArrayList<CaseHyperlinkData>();
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
    public List<CaseHyperlinkData> getCase_hyperlink_data() {
        return case_hyperlink_data;
    }
    public void setCase_hyperlink_data(List<CaseHyperlinkData> case_hyperlink_data) {
        this.case_hyperlink_data = case_hyperlink_data;
    }
}