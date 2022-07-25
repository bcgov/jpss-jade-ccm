import java.util.List;

public class JustinEvent {
  private int event_message_id;
  private String appl_application_cd;
  private String message_event_type_cd;
  private String event_dtm;
  private List<JustinEventData> event_data;

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

  public List<JustinEventData> getEvent_data() {
    return this.event_data;
  }

  public void setEvent_data(List<JustinEventData> event_data) {
    this.event_data = event_data;
  }
}