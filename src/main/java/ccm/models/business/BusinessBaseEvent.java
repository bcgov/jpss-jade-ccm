package ccm.models.business;

public class BusinessBaseEvent {
    private String event_id;
    private String event_dtm;
    private String event_version;

    public BusinessBaseEvent() {
    }

    public BusinessBaseEvent(BusinessBaseEvent another) {
        this.event_id = another.event_id;
        this.event_dtm = another.event_dtm;
        this.event_version = another.event_version;
    }

    public String getEvent_id() {
        return event_id;
    }
    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }
    public String getEvent_version() {
        return event_version;
    }
    public void setEvent_version(String event_version) {
        this.event_version = event_version;
    }
    public String getEvent_dtm() {
        return event_dtm;
    }
    public void setEvent_dtm(String event_dtm) {
        this.event_dtm = event_dtm;
    }
}
