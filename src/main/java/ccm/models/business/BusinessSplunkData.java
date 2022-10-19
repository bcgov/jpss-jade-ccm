package ccm.models.business;

public class BusinessSplunkData {
    public static final String DEFAULT_SOURCE_TYPE = "manual";

    private String event;
    private String sourcetype;

    public BusinessSplunkData() {
    }

    public BusinessSplunkData(BusinessSplunkEvent se) {
        setEvent(se.getEvent_message());
        if(se.getSource() != null) {
            setSourcetype(se.getSource());
        } else {
            setSourcetype(DEFAULT_SOURCE_TYPE);
        }
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
