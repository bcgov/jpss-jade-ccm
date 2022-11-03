package ccm.models.common.event;

import ccm.utils.DateTimeUtils;

public class EventKPI {
  public enum STATUS {
    EVENT_CREATING,
    EVENT_CREATED,
    EVENT_PROCESSING_STARTED,
    EVENT_PROCESSING_COMPLETED,
    EVENT_PROCESSING_FAILED,
    UNKNOWN;
  }

  private String kpi_dtm;
  private String kpi_version;
  private String kpi_status;
  private String event_type;
  private String event_key;
  private String event_status;
  private Object event_details;

  private String integration_component_name;
  private String component_route_name;

  public static final String KPI_VERSION = "1.0";

  public EventKPI() {
    setKpi_dtm(DateTimeUtils.generateCurrentDtm());
    setKpi_version(KPI_VERSION);
  }

  public EventKPI(BaseEvent event) {
    this();
    setEvent_details(event);
    setEvent_type(event.getEvent_type());
    setEvent_key(event.getEvent_key());
    setEvent_status(event.getEvent_status());
  }

  public EventKPI(BaseEvent event, STATUS status) {
    this(event);
    setKpi_status(status.name());
  }

  public EventKPI(BaseEvent event, String suggested_status_string) {
    this(event);

    String final_status_string = null;

    try {
      STATUS status = STATUS.valueOf(suggested_status_string);
      final_status_string = status.name();
    } catch (IllegalArgumentException e) {
      final_status_string = STATUS.UNKNOWN.name() + "(" + suggested_status_string + ")";
    }

    setKpi_status(final_status_string);
  }

  public String getKpi_dtm() {
    return kpi_dtm;
  }

  public void setKpi_dtm(String kpi_dtm) {
    this.kpi_dtm = kpi_dtm;
  }

  public String getKpi_version() {
    return kpi_version;
  }

  public void setKpi_version(String kpi_version) {
    this.kpi_version = kpi_version;
  }

  public String getKpi_status() {
    return kpi_status;
  }

  public void setKpi_status(String kpi_status) {
    this.kpi_status = kpi_status;
  }

  public String getEvent_type() {
    return event_type;
  }

  public void setEvent_type(String event_type) {
    this.event_type = event_type;
  }

  public String getEvent_key() {
    return event_key;
  }

  public void setEvent_key(String event_key) {
    this.event_key = event_key;
  }

  public String getEvent_status() {
    return event_status;
  }

  public void setEvent_status(String event_status) {
    this.event_status = event_status;
  }

  public String getComponent_route_name() {
    return component_route_name;
  }

  public void setComponent_route_name(String component_route_id) {
    this.component_route_name = component_route_id;
  }

  public Object getEvent_details() {
    return event_details;
  }

  public void setEvent_details(Object event_details) {
    this.event_details = event_details;
  }

  public String getIntegration_component_name() {
    return integration_component_name;
  }

  public void setIntegration_component_name(String integration_component_name) {
    this.integration_component_name = integration_component_name;
  }
}