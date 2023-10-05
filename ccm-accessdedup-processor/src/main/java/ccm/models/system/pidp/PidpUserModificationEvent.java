package ccm.models.system.pidp;

public class PidpUserModificationEvent {
  private String eventTime;
  private String partId;
  private String eventType;
  private Boolean successful;
  private Long accessRequestId;
  private Boolean submittingAgencyUser;
  
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
  public String getEventType() {
    return eventType;
  }
  public void setEventType(String event) {
    this.eventType = event;
  }
  public Long getAccessRequestId() {
    return this.accessRequestId;
  }
  public void setAccessRequestId(Long accessRequestId) {
    this.accessRequestId = accessRequestId;
  }
  public Boolean getSuccessful() {
    return successful;
  }
  public void setSuccessful(Boolean success) {
    this.successful = success;
  }
  public Boolean getSubmittingAgencyUser() {
    return submittingAgencyUser;
  }
  public void setSubmittingAgencyUser(Boolean submittingAgencyUser) {
    this.submittingAgencyUser = submittingAgencyUser;
  }

}
  