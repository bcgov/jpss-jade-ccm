package ccm.models.common.event;

import java.util.Iterator;
import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;

public class ChargeAssessmentEvent extends BaseEvent {
  private int justin_event_message_id;
  private String justin_message_event_type_cd;
  private String justin_event_dtm;
  private String justin_fetched_date;
  private String justin_guid;
  private String justin_rcc_id;
  private String justin_part_id;

  public static final String JUSTIN_FETCHED_DATE = "FETCHED_DATE";
  public static final String JUSTIN_GUID = "GUID";
  public static final String JUSTIN_RCC_ID = "RCC_ID";
  public static final String JUSTIN_PART_ID = "PART_ID";

  public enum SOURCE {
    JUSTIN,
    JADE_CCM
  }
  
  public enum STATUS {
    CHANGED,
    CREATED,
    UPDATED,
    MANUALLY_CHANGED,
    AUTH_LIST_CHANGED,
    INFERRED_AUTH_LIST_CHANGED,
    INFERRED_PART_ID_PROVISIONED
  }

  public ChargeAssessmentEvent() {
    super();
  }

  public ChargeAssessmentEvent(JustinEvent je) {
    this();

    setEvent_source(SOURCE.JUSTIN.toString());

    setJustin_event_message_id(je.getEvent_message_id());
    setJustin_message_event_type_cd(je.getMessage_event_type_cd());
    setJustin_event_dtm(je.getEvent_dtm());

    switch(JustinEvent.STATUS.valueOf(je.getMessage_event_type_cd())) {
      case AGEN_FILE:
        setEvent_status(STATUS.CHANGED.toString());
        break;
      case MANU_FILE:
        setEvent_status(STATUS.MANUALLY_CHANGED.toString());
        break;
      case AUTH_LIST:
        setEvent_status(STATUS.AUTH_LIST_CHANGED.toString());
        break;
      default:
        // unknown status
        setEvent_status("");
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
      setEvent_key(getJustin_rcc_id());
  }

  public ChargeAssessmentEvent(SOURCE source, ChargeAssessmentEvent another) {
    super(source.name(), another);

    this.justin_event_message_id = another.justin_event_message_id;
    this.justin_message_event_type_cd = another.justin_message_event_type_cd;
    this.justin_event_dtm = another.justin_event_dtm;
    this.justin_fetched_date = another.justin_fetched_date;
    this.justin_guid = another.justin_guid;
    this.justin_rcc_id = another.justin_rcc_id;
  }

  public ChargeAssessmentEvent(SOURCE source, CaseUserEvent caseUserEvent) {
    super();

    this.setEvent_key(caseUserEvent.getJustin_rcc_id());
    this.setEvent_source(source.name());
    this.setEvent_status(ChargeAssessmentEvent.STATUS.INFERRED_AUTH_LIST_CHANGED.name());
    this.setJustin_event_dtm(caseUserEvent.getJustin_event_dtm());
    this.setJustin_event_message_id(caseUserEvent.getJustin_event_message_id());
    this.setJustin_fetched_date(caseUserEvent.getJustin_fetched_date());
    this.setJustin_guid(caseUserEvent.getJustin_guid());
    this.setJustin_message_event_type_cd(caseUserEvent.getJustin_message_event_type_cd());
    this.setJustin_rcc_id(caseUserEvent.getJustin_rcc_id());
    this.setJustin_part_id(caseUserEvent.getJustin_part_id());
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

  public String getJustin_part_id() {
    return justin_part_id;
  }

  public void setJustin_part_id(String justin_part_id) {
    this.justin_part_id = justin_part_id;
  }

}