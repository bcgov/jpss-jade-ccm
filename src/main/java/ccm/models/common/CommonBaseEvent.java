package ccm.models.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonBaseEvent {
    private String event_dtm;
    private String event_version;
    private String event_type;
    private String event_status;
    private String event_source;
    private String event_object_id;
    private CommonEventError event_error;

    public CommonBaseEvent() {
        this.event_dtm = util_generateCurrentDtm();
        this.event_type = this.getClass().getSimpleName();
    }

    public CommonBaseEvent(CommonBaseEvent another) {
        this.event_dtm = another.event_dtm;
        this.event_version = another.event_version;
        this.event_type = another.event_type;
        this.event_source = another.event_source;
        this.event_status = another.event_status;
        this.event_object_id = another.event_object_id;
    }

    public String getEvent_object_id() {
        return event_object_id;
    }
    public void setEvent_object_id(String event_object_id) {
        this.event_object_id = event_object_id;
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

    public CommonEventError getEvent_error() {
        return event_error;
    }

    public void setEvent_error(CommonEventError event_error) {
        this.event_error = event_error;
    }

    public static String util_generateCurrentDtm() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        
        return formatter.format(date);
    }
}
