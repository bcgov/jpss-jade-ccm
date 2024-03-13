package ccm.models.system.pidp;

import ccm.models.common.event.CaseUserEvent;
import ccm.utils.DateTimeUtils;

public class PidpUserProcessStatusEvent {
  public static final String DOMAIN_ACCOUNT_FINALIZED = "digitalevidence-bcps-usercreation-accountfinalized";
  public static final String STATUS_SUCCESS = "success";

  private String eventTime;
  private String partId;
  private String traceId;
  private String domainEvent;
  private String status;
  
  public PidpUserProcessStatusEvent(CaseUserEvent cue) {
    setEventTime(DateTimeUtils.generateCurrentDtm());
    setPartId(cue.getJustin_part_id());
    setTraceId(String.valueOf(cue.getJustin_event_message_id()));
    setDomainEvent(DOMAIN_ACCOUNT_FINALIZED);
    setStatus(STATUS_SUCCESS);
  }
  
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
  public String getTraceId() {
    return traceId;
  }
  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getDomainEvent() {
    return domainEvent;
  }
  public void setDomainEvent(String domainEvent) {
    this.domainEvent = domainEvent;
  }

  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

}
