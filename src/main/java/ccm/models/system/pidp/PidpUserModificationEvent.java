package ccm.models.system.pidp;

import java.util.Date;

public class PidpUserModificationEvent {
  private Date EventTime;
  private String PartId;
  private String Event;
  private Integer AccessRequestId;
  
  public Date getEventTime() {
    return EventTime;
  }
  public void setEventTime(Date eventTime) {
    EventTime = eventTime;
  }
  public String getPartId() {
    return PartId;
  }
  public void setPartId(String partId) {
    PartId = partId;
  }
  public String getEvent() {
    return Event;
  }
  public void setEvent(String event) {
    Event = event;
  }
  public Integer getAccessRequestId() {
    return AccessRequestId;
  }
  public void setAccessRequestId(Integer accessRequestId) {
    AccessRequestId = accessRequestId;
  }

}
  