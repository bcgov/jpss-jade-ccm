package ccm.models.common.event;

import ccm.utils.DateTimeUtils;

public class EventError {
    private String error_dtm;
    private String error_version;
    private String error_code;
    private String error_summary;
    private Object error_details;

    public static final String ERROR_VERSION = "0.2";

    public EventError() {
        setError_dtm(DateTimeUtils.generateCurrentDtm());
    }

    public EventError(EventError another) {
        this.error_dtm = another.error_dtm;
        this.error_code = another.error_code;
        this.error_details = another.error_details;
    }

    public String getError_dtm() {
        return error_dtm;
    }

    public void setError_dtm(String error_dtm) {
        this.error_dtm = error_dtm;
    }

    public String getError_code() {
        return error_code;
    }

    public void setError_code(String error_code) {
        this.error_code = error_code;
    }

    public Object getError_details() {
        return error_details;
    }

    public void setError_details(Object error_details) {
        this.error_details = error_details;
    }

    public String getError_version() {
        return error_version;
    }

    public void setError_version(String error_version) {
        this.error_version = error_version;
    }

    public String getError_summary() {
        return error_summary;
    }

    public void setError_summary(String error_summary) {
        this.error_summary = error_summary;
    }
}
