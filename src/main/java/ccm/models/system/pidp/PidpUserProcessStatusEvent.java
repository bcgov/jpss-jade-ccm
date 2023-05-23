package ccm.models.system.pidp;

import ccm.models.common.event.CaseUserEvent;
import ccm.utils.DateTimeUtils;

public class PidpUserProcessStatusEvent {
  private String eventTime;
  private String id;
  private String traceId;
  
  public PidpUserProcessStatusEvent(CaseUserEvent cue) {
    setEventTime(DateTimeUtils.generateCurrentDtm());
    setId(cue.getJustin_part_id());
    setTraceId(String.valueOf(cue.getJustin_event_message_id()));
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
  