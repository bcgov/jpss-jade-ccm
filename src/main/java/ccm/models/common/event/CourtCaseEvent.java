package ccm.models.common.event;

import java.util.Iterator;
import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;

public class CourtCaseEvent extends BaseEvent {
  private int justin_event_message_id;
  private String justin_message_event_type_cd;
  private String justin_event_dtm;
  private String justin_fetched_date;
  private String justin_guid;
  private String justin_mdoc_no;

  public static final String JUSTIN_FETCHED_DATE = "FETCHED_DATE";
  public static final String JUSTIN_GUID = "GUID";
  public static final String JUSTIN_MDOC_NO = "MDOC_JUSTIN_NO";

  public enum SOURCE {
    JUSTIN,
    ISL_CCM;
  }

  public enum STATUS {
    CHANGED,
    MANUALLY_CHANGED,
    APPEARANCE_CHANGED,
    CROWN_ASSIGNMENT_CHANGED
  }

  public CourtCaseEvent() {
    super();
  }

  public CourtCaseEvent(JustinEvent je) {
    this();

    setEvent_source(SOURCE.JUSTIN.toString());

    setJustin_event_message_id(je.getEvent_message_id());
    setJustin_message_event_type_cd(je.getMessage_event_type_cd());
    setJustin_event_dtm(je.getEvent_dtm());

    switch(JustinEvent.STATUS.valueOf(je.getMessage_event_type_cd())) {
      case COURT_FILE:
        setEvent_status(STATUS.CHANGED.toString());
        break;
      case MANU_CFILE:
        setEvent_status(STATUS.MANUALLY_CHANGED.toString());
        break;
      case APPR:
        setEvent_status(STATUS.APPEARANCE_CHANGED.toString());
        break;
      case CRN_ASSIGN:
        setEvent_status(STATUS.CROWN_ASSIGNMENT_CHANGED.toString());
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
        case JUSTIN_MDOC_NO:
          setJustin_mdoc_no(jed.getData_value_txt());
          break;
      }
    }

    setEvent_key(getJustin_mdoc_no());
  }

  public CourtCaseEvent(String event_source, CourtCaseEvent another) {
    super(event_source, another);

    this.justin_event_message_id = another.justin_event_message_id;
    this.justin_message_event_type_cd = another.justin_message_event_type_cd;
    this.justin_event_dtm = another.justin_event_dtm;
    this.justin_fetched_date = another.justin_fetched_date;
    this.justin_guid = another.justin_guid;
    this.justin_mdoc_no = another.justin_mdoc_no;
  }

  public CourtCaseEvent copy(String event_source) {
    return new CourtCaseEvent(event_source, this);
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

  public String getJustin_mdoc_no() {
    return justin_mdoc_no;
  }

  public void setJustin_mdoc_no(String justin_mdoc_no) {
    this.justin_mdoc_no = justin_mdoc_no;
  }

  
}