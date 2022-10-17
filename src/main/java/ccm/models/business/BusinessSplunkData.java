package ccm.models.business;

public class BusinessSplunkData {
    private String event;
    private String sourcetype;

    public BusinessSplunkData() {
    }

    public BusinessSplunkData(BusinessSplunkEvent se) {
        setEvent(se.getEvent_message());
        setSourcetype("manual");
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSourcetype() {
        return sourcetype;
    }

    public void setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
    }

}
