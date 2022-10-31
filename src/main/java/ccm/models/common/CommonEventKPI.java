package ccm.models.common;

public class CommonEventKPI {
  public enum STATUS {
    CREATING,
    CREATED,
    PROCESSING_STARTED,
    PROCESSING_COMPLETED,
    PROCESSING_FAILED;
  }

  private String event_dtm;
  private String event_version;
  private String event_type;
  private String event_status;
  private String event_source;
  private String event_object_id;

  private String application_component_name;
  private String component_route_id;
  private CommonChargeAssessmentCaseEvent event;

  public static final String EVENT_VERSION = "1.0";

  public CommonEventKPI() {
    super();
    setEvent_version(EVENT_VERSION);
  }

  public String getEvent_object_id() {
    return event_object_id;
  }

  public void setEvent_object_id(String event_object_id) {
    this.event_object_id = event_object_id;
  }

  public String getEvent_version() {
    return event_version;
  }

  public void setEvent_version(String event_version) {
    this.event_version = event_version;
  }

  public String getEvent_dtm() {
    return event_dtm;
  }

  public void setEvent_dtm(String event_dtm) {
    this.event_dtm = event_dtm;
  }

  public String getEvent_type() {
    return event_type;
  }

  public void setEvent_type(String event_type) {
    this.event_type = event_type;
  }

  public String getEvent_status() {
    return event_status;
  }

  public void setEvent_status(String event_status) {
    this.event_status = event_status;
  }

  public String getEvent_source() {
    return event_source;
  }

  public void setEvent_source(String event_source) {
    this.event_source = event_source;
  }

  public CommonEventKPI(CommonChargeAssessmentCaseEvent event) {
    this();
    setEvent(event);
    setEvent_object_id(event.getEvent_type() + "-" + event.getEvent_object_id() + "-" + event.getEvent_status());
  }

  public CommonEventKPI(CommonChargeAssessmentCaseEvent event, STATUS status) {
    this(event);
    setEvent_status(status.name());
  }

  public String getComponent_route_id() {
    return component_route_id;
  }

  public void setComponent_route_id(String component_route_id) {
    this.component_route_id = component_route_id;
  }

  public CommonChargeAssessmentCaseEvent getEvent() {
    return event;
  }

  public void setEvent(CommonChargeAssessmentCaseEvent event) {
    this.event = event;
  }

  public String getApplication_component_name() {
    return application_component_name;
  }

  public void setApplication_component_name(String application_component_name) {
    this.application_component_name = application_component_name;
  }
}