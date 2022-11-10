package ccm.models.common.event;

import ccm.models.common.versioning.Version;
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
  private Object event_details;
  private String event_topic_name;
  private Long event_topic_offset;
  private Error error;

  private String integration_component_name;
  private String component_route_name;

  public EventKPI() {
    setKpi_dtm(DateTimeUtils.generateCurrentDtm());
    setKpi_version(Version.V1_0.toString());
  }

  public EventKPI(BaseEvent event) {
    this();
    setEvent_details(event);
  }

  public EventKPI(STATUS status) {
    this();
    setKpi_status(status.name());
  }

  public EventKPI(String suggested_status_string) {
    this();

    setKpi_status(util_getStatusString(suggested_status_string));
  }

  public EventKPI(BaseEvent event, STATUS status) {
    this(event);
    setKpi_status(status.name());
  }

  public EventKPI(BaseEvent event, String suggested_status_string) {
    this(event);

    setKpi_status(util_getStatusString(suggested_status_string));
  }

  private static String util_getStatusString(String suggested_status_string) {
    String final_status_string = null;

    try {
      STATUS status = STATUS.valueOf(suggested_status_string);
      final_status_string = status.name();
    } catch (IllegalArgumentException e) {
      final_status_string = STATUS.UNKNOWN.name() + "(" + suggested_status_string + ")";
    }

    return final_status_string;
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

  public String getEvent_topic_name() {
    return event_topic_name;
  }

  public void setEvent_topic_name(String event_topic_name) {
    this.event_topic_name = event_topic_name;
  }

  public Long getEvent_topic_offset() {
    return event_topic_offset;
  }

  public void setEvent_topic_offset(Long event_topic_offset) {
    this.event_topic_offset = event_topic_offset;
  }

  public Error getError() {
    return error;
  }

  public void setError(Error error) {
    this.error = error;
  }
}