package ccm.models.common;

public class CommonKPIEvent extends CommonBaseEvent {
  public enum STATUS {
    CREATED,
    PROCESSED,
    NOT_PROCESSED;
  }

  private String kpi_dtm;
  private String kpi_status;
  private String application_component_name;
  private String component_route_id;
  private CommonBaseEvent event;

  public CommonKPIEvent(CommonBaseEvent event) {
    super();
    setKpi_dtm(CommonBaseEvent.util_generateCurrentDtm());
    setEvent(event);
  }

  public String getKpi_dtm() {
    return kpi_dtm;
  }

  public void setKpi_dtm(String kpi_dtm) {
    this.kpi_dtm = kpi_dtm;
  }

  public String getKpi_status() {
    return kpi_status;
  }

  public void setKpi_status(String kpi_status) {
    this.kpi_status = kpi_status;
  }

  public String getComponent_route_id() {
    return component_route_id;
  }

  public void setComponent_route_id(String component_route_id) {
    this.component_route_id = component_route_id;
  }

  public CommonBaseEvent getEvent() {
    return event;
  }

  public void setEvent(CommonBaseEvent event) {
    this.event = event;
  }

  public String getApplication_component_name() {
    return application_component_name;
  }

  public void setApplication_component_name(String application_component_name) {
    this.application_component_name = application_component_name;
  }
}