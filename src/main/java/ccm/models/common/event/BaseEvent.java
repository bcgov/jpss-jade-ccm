package ccm.models.common.event;

import ccm.models.common.versioning.Version;
import ccm.utils.DateTimeUtils;

public class BaseEvent {
    private String event_dtm;
    private String event_version;
    private String event_type;
    private String event_status;
    private String event_source;
    private String event_key;

    public BaseEvent() {
        this.event_dtm = DateTimeUtils.generateCurrentDtm();
        this.event_type = this.getClass().getSimpleName();
        this.event_version = Version.V1_0.toString();
    }

    public BaseEvent(BaseEvent another) {
        this.event_dtm = another.event_dtm;
        this.event_version = another.event_version;
        this.event_type = another.event_type;
        this.event_source = another.event_source;
        this.event_status = another.event_status;
        this.event_key = another.event_key;
    }

    public String getEvent_key() {
        return event_key;
    }
    public void setEvent_key(String event_key) {
        this.event_key = event_key;
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

    public String getEvent_type() {
        return event_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public String getEvent_status() {
        return event_status;
    }

    public void setEvent_status(String event_status) {
        this.event_status = event_status;
    }

    public String getEvent_source() {
        return event_source;
    }

    public void setEvent_source(String event_source) {
        this.event_source = event_source;
    }
}
