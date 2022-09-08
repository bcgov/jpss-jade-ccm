package ccm.models.business;

import java.util.Iterator;
import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;

public class BusinessCourtCaseEvent extends BusinessBaseEvent {
  private String event_status;
  private String event_source;

  private int justin_event_message_id;
  private String justin_message_event_type_cd;
  private String justin_event_dtm;
  private String justin_fetched_date;
  private String justin_guid;
  private String justin_rcc_id;

  public static final String COURT_CASE_EVENT_VERSION = "1.0";

  public static final String STATUS_CHANGED = "CHANGED";
  public static final String STATUS_CREATED = "CREATED";
  public static final String STATUS_UPDATED = "UPDATED";
  public static final String STATUS_AUTH_LIST_CHANGED = "AUTH_LIST_CHANGED";

  public static final String SOURCE_JUSTIN = "JUSTIN";
  public static final String SOURCE_JADE_CCM = "JADE-CCM";

  public static final String JUSTIN_FETCHED_DATE = "FETCHED_DATE";
  public static final String JUSTIN_GUID = "GUID";
  public static final String JUSTIN_RCC_ID = "RCC_ID";

  public BusinessCourtCaseEvent() {
    super.setEvent_version(COURT_CASE_EVENT_VERSION);
  }

  public BusinessCourtCaseEvent(JustinEvent je) {
    this();

    setEvent_source(SOURCE_JUSTIN);

    setJustin_event_message_id(je.getEvent_message_id());
    setJustin_message_event_type_cd(je.getMessage_event_type_cd());

    switch(je.getMessage_event_type_cd()) {
      case JustinEvent.EVENT_TYPE_AGEN_FILE:
        setEvent_status(STATUS_CHANGED);
        break;
      case JustinEvent.EVENT_TYPE_AUTH_LIST:
        setEvent_status(STATUS_AUTH_LIST_CHANGED);
        break;
    }
    
    Iterator<JustinEventDataElement> i = je.getEvent_data().iterator();
    while(i.hasNext()) {
      JustinEventDataElement jed = i.next();

      switch(jed.getData_element_nm()) {
        case JUSTIN_FETCHED_DATE:
          setJustin_fetched_date(jed.getData_value_txt());
          break;
        case JUSTIN_GUID:
          setJustin_guid(jed.getData_value_txt());
          break;
        case JUSTIN_RCC_ID:
          setJustin_rcc_id(jed.getData_value_txt());
          break;
      }
    }

    setEvent_object_id(getJustin_rcc_id());
  }

  public BusinessCourtCaseEvent(String event_source, BusinessCourtCaseEvent another) {
    super((BusinessBaseEvent)another);

    setEvent_source(event_source);

    this.event_status = another.event_status;
    this.justin_event_message_id = another.justin_event_message_id;
    this.justin_message_event_type_cd = another.justin_message_event_type_cd;
    this.justin_event_dtm = another.justin_event_dtm;
    this.justin_fetched_date = another.justin_fetched_date;
    this.justin_guid = another.justin_guid;
    this.justin_rcc_id = another.justin_rcc_id;
  }

  public BusinessCourtCaseEvent copy(String event_source) {
    return new BusinessCourtCaseEvent(event_source, this);
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

  public int getJustin_event_message_id() {
    return justin_event_message_id;
  }

  public void setJustin_event_message_id(int justin_event_message_id) {
    this.justin_event_message_id = justin_event_message_id;
  }

  public String getJustin_message_event_type_cd() {
    return justin_message_event_type_cd;
  }

  public void setJustin_message_event_type_cd(String justin_message_event_type_cd) {
    this.justin_message_event_type_cd = justin_message_event_type_cd;
  }

  public String getJustin_event_dtm() {
    return justin_event_dtm;
  }

  public void setJustin_event_dtm(String justin_event_dtm) {
    this.justin_event_dtm = justin_event_dtm;
  }

  public String getJustin_fetched_date() {
    return justin_fetched_date;
  }

  public void setJustin_fetched_date(String justin_fetched_date) {
    this.justin_fetched_date = justin_fetched_date;
  }

  public String getJustin_guid() {
    return justin_guid;
  }

  public void setJustin_guid(String justin_guid) {
    this.justin_guid = justin_guid;
  }

  public String getJustin_rcc_id() {
    return justin_rcc_id;
  }

  public void setJustin_rcc_id(String justin_rcc_id) {
    this.justin_rcc_id = justin_rcc_id;
  }
}