package ccm.models.common;

public class CommonKPIEvent extends CommonBaseEvent {
  public enum STATUS {
    CREATING,
    CREATED,
    PROCESSING_STARTED,
    PROCESSING_COMPLETED,
    PROCESSING_FAILED;
  }

  private String application_component_name;
  private String component_route_id;
  private CommonCourtCaseEvent event;

  public static final String EVENT_VERSION = "1.0";

  public CommonKPIEvent() {
    super();
    setEvent_version(EVENT_VERSION);
  }

  public CommonKPIEvent(CommonCourtCaseEvent event) {
    this();
    setEvent(event);
    setEvent_object_id(event.getEvent_type() + "-" + event.getEvent_object_id() + "-" + event.getEvent_status());
  }

  public CommonKPIEvent(CommonCourtCaseEvent event, STATUS status) {
    this(event);
    setEvent_status(status.name());
  }

  public String getComponent_route_id() {
    return component_route_id;
  }

  public void setComponent_route_id(String component_route_id) {
    this.component_route_id = component_route_id;
  }

  public CommonCourtCaseEvent getEvent() {
    return event;
  }

  public void setEvent(CommonCourtCaseEvent event) {
    this.event = event;
  }

  public String getApplication_component_name() {
    return application_component_name;
  }

  public void setApplication_component_name(String application_component_name) {
    this.application_component_name = application_component_name;
  }
}