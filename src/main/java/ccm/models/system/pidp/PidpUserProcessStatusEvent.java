package ccm.models.system.pidp;

import ccm.models.common.event.CaseUserEvent;

public class PidpUserProcessStatusEvent {
  private String eventTime;
  private String id;
  private String traceId;
  
  public PidpUserProcessStatusEvent(CaseUserEvent cue) {

  }
  
  public String getEventTime() {
    return eventTime;
  }
  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getTraceId() {
    return traceId;
  }
  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

}
  