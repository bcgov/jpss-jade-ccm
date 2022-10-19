package ccm.models.business;


public class BusinessSplunkEvent extends BusinessBaseEvent {
  private String event_message;
  private String source;

  public static final String EVENT_VERSION = "1.0";

  public BusinessSplunkEvent() {
    super.setEvent_version(EVENT_VERSION);
  }

  public BusinessSplunkEvent(String event_message) {
    this();
    setEvent_message(event_message);
  }

  public String getEvent_message() {
    return event_message;
  }

  public void setEvent_message(String event_message) {
    this.event_message = event_message;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }


}