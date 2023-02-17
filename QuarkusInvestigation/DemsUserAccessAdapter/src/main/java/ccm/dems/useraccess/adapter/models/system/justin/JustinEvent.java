package ccm.dems.useraccess.adapter.models.system.justin;

import java.util.List;

public class JustinEvent {
    private int event_message_id;
    private String appl_application_cd;
    private String message_event_type_cd;
    private String message_event_type_dsc;
    private String process_retry_qty;
    private String maximum_retry_qty;
    private String process_status_cd;
    private String process_status_dsc;
    private String event_dtm;
    private List<JustinEventDataElement> event_data;

    public enum STATUS {
        AGEN_FILE, AUTH_LIST, COURT_FILE, APPR, CRN_ASSIGN, MANU_FILE, MANU_CFILE, USER_PROV, USER_DPROV;
    }

    public boolean isAgenFileEvent() {
        return STATUS.AGEN_FILE.equals(getMessage_event_type_cd());
    }

    public boolean isManuFileEvent() {
        return STATUS.MANU_FILE.equals(getMessage_event_type_cd());
    }

    public boolean isManuCfileEvent() {
        return STATUS.MANU_CFILE.equals(getMessage_event_type_cd());
    }

    public boolean isCourtFileEvent() {
        return STATUS.COURT_FILE.equals(getMessage_event_type_cd());
    }

    public boolean isAuthListEvent() {
        return STATUS.AUTH_LIST.equals(getMessage_event_type_cd());
    }

    public boolean isApprEvent() {
        return STATUS.APPR.equals(getMessage_event_type_cd());
    }

    public boolean isCrownAsgnEvent() {
        return STATUS.CRN_ASSIGN.equals(getMessage_event_type_cd());
    }

    public boolean isUserProvEvent() {
        return STATUS.USER_PROV.equals(getMessage_event_type_cd());
    }

    public boolean isUserDProvEvent() {
        return STATUS.USER_DPROV.equals(getMessage_event_type_cd());
    }

    public String getMessage_event_type_dsc() {
        return message_event_type_dsc;
    }

    public void setMessage_event_type_dsc(String message_event_type_dsc) {
        this.message_event_type_dsc = message_event_type_dsc;
    }

    public String getProcess_retry_qty() {
        return process_retry_qty;
    }

    public void setProcess_retry_qty(String process_retry_qty) {
        this.process_retry_qty = process_retry_qty;
    }

    public String getMaximum_retry_qty() {
        return maximum_retry_qty;
    }

    public void setMaximum_retry_qty(String maximum_retry_qty) {
        this.maximum_retry_qty = maximum_retry_qty;
    }

    public String getProcess_status_cd() {
        return process_status_cd;
    }

    public void setProcess_status_cd(String process_status_cd) {
        this.process_status_cd = process_status_cd;
    }

    public String getProcess_status_dsc() {
        return process_status_dsc;
    }

    public void setProcess_status_dsc(String process_status_dsc) {
        this.process_status_dsc = process_status_dsc;
    }

    public int getEvent_message_id() {
        return this.event_message_id;
    }

    public void setEvent_message_id(int event_message_id) {
        this.event_message_id = event_message_id;
    }

    public String getAppl_application_cd() {
        return this.appl_application_cd;
    }

    public void setAppl_application_cd(String appl_application_cd) {
        this.appl_application_cd = appl_application_cd;
    }

    public String getMessage_event_type_cd() {
        return this.message_event_type_cd;
    }

    public void setMessage_event_type_cd(String message_event_type_cd) {
        this.message_event_type_cd = message_event_type_cd;
    }

    public String getEvent_dtm() {
        return this.event_dtm;
    }

    public void setEvent_dtm(String event_dtm) {
        this.event_dtm = event_dtm;
    }

    public List<JustinEventDataElement> getEvent_data() {
        return this.event_data;
    }

    public void setEvent_data(List<JustinEventDataElement> event_data) {
        this.event_data = event_data;
    }
}
