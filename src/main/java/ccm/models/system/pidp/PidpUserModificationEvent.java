package ccm.models.system.pidp;

public class PidpUserModificationEvent {
  private String eventTime;
  private String partId;
  private String event;
  private Long accessRequestId;
  
  public String getEventTime() {
    return eventTime;
  }
  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }
  public String getPartId() {
    return partId;
  }
  public void setPartId(String partId) {
    this.partId = partId;
  }
  public String getEvent() {
    return event;
  }
  public void setEvent(String event) {
    this.event = event;
  }
  public Long getAccessRequestId() {
    return this.accessRequestId;
  }
  public void setAccessRequestId(Long accessRequestId) {
    this.accessRequestId = accessRequestId;
  }

}
  