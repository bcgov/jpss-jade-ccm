package ccm.models.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonBaseEvent {
    private String event_dtm;
    private String event_version;
    private String event_object_id;
    private CommonEventErrorData event_error_data;

    public CommonBaseEvent() {
        this.event_dtm = util_generateCurrentDtm();
    }

    public CommonBaseEvent(CommonBaseEvent another) {
        this.event_object_id = another.event_object_id;
        this.event_dtm = another.event_dtm;
        this.event_version = another.event_version;
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

    public CommonEventErrorData getEvent_error_data() {
        return event_error_data;
    }

    public void setEvent_error_data(CommonEventErrorData event_error_data) {
        this.event_error_data = event_error_data;
    }

    public static String util_generateCurrentDtm() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        
        return formatter.format(date);
    }
}
