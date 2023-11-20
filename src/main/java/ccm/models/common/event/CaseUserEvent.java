package ccm.models.common.event;

import java.util.Iterator;
import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;
import ccm.models.system.pidp.PidpUserModificationEvent;

public class CaseUserEvent extends BaseEvent {
  private int justin_event_message_id;
  private String justin_message_event_type_cd;
  private String justin_event_dtm;
  private String justin_fetched_date;
  private String justin_guid;
  private String justin_part_id;
  private String justin_rcc_id;
  private String justin_record_cnt;
  private String justin_record_num;

  private String pidp_event_time;
  private Long pidp_access_request_id;

  public static final String JUSTIN_FETCHED_DATE = "FETCHED_DATE";
  public static final String JUSTIN_GUID = "GUID";
  public static final String JUSTIN_PART_ID = "PART_ID";
  public static final String JUSTIN_RCC_ID = "RCC_ID";
  public static final String JUSTIN_RECORD_CNT = "RECORD_CNT";
  public static final String JUSTIN_RECORD_NUM = "RECORD_NUM";

  public enum SOURCE {
    JUSTIN,
    PIDP,
    JADE_CCM
  }
  
  public enum STATUS {
    ACCOUNT_CREATED,
    ACCESS_ADDED,
    ACCESS_REMOVED,
    EVENT_BATCH_ENDED;
  }

  public CaseUserEvent() {
    super();
  }

  public CaseUserEvent(JustinEvent je) {
    this();

    setEvent_source(SOURCE.JUSTIN.toString());

    setJustin_event_message_id(je.getEvent_message_id());
    setJustin_message_event_type_cd(je.getMessage_event_type_cd());
    setJustin_event_dtm(je.getEvent_dtm());

    switch(JustinEvent.STATUS.valueOf(je.getMessage_event_type_cd())) {
      case USER_PROV:
        setEvent_status(STATUS.ACCESS_ADDED.toString());
        break;
      case USER_DPROV:
        setEvent_status(STATUS.ACCESS_REMOVED.toString());
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
        case JUSTIN_PART_ID:
          setJustin_part_id(jed.getData_value_txt());
          break;
        case JUSTIN_RCC_ID:
          setJustin_rcc_id(jed.getData_value_txt());
          break;
        case JUSTIN_RECORD_CNT:
          setJustin_record_cnt(jed.getData_value_txt());
          break;
        case JUSTIN_RECORD_NUM:
          setJustin_record_num(jed.getData_value_txt());
          break;
      }
    }

    setEvent_key(getJustin_part_id());
  }

  public CaseUserEvent(SOURCE source, CaseUserEvent another) {
    super(source.name(), another);

    this.justin_event_message_id = another.justin_event_message_id;
    this.justin_message_event_type_cd = another.justin_message_event_type_cd;
    this.justin_event_dtm = another.justin_event_dtm;
    this.justin_fetched_date = another.justin_fetched_date;
    this.justin_guid = another.justin_guid;
    this.justin_part_id = another.justin_part_id;
  }

  public CaseUserEvent(PidpUserModificationEvent pidpEvent) {
    this();

    this.setEvent_source(SOURCE.PIDP.name());
    this.setEvent_status(STATUS.ACCOUNT_CREATED.name());
    this.setEvent_key(pidpEvent.getPartId());
    this.setPidp_event_time(pidpEvent.getEventTime());
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

  public String getJustin_part_id() {
    return justin_part_id;
  }

  public void setJustin_part_id(String justin_part_id) {
    this.justin_part_id = justin_part_id;
  }

  public String getJustin_rcc_id() {
    return justin_rcc_id;
  }

  public void setJustin_rcc_id(String justin_rcc_id) {
    this.justin_rcc_id = justin_rcc_id;
  }

  public String getPidp_event_time() {
    return pidp_event_time;
  }

  public void setPidp_event_time(String pidp_event_time) {
    this.pidp_event_time = pidp_event_time;
  }

  public Long getPidp_access_request_id() {
    return pidp_access_request_id;
  }

  public void setPidp_access_request_id(Long pidp_access_request_id) {
    this.pidp_access_request_id = pidp_access_request_id;
  }

  public String getJustin_record_cnt() {
    return justin_record_cnt;
  }

  public void setJustin_record_cnt(String justin_record_cnt) {
    this.justin_record_cnt = justin_record_cnt;
  }

  public String getJustin_record_num() {
    return justin_record_num;
  }

  public void setJustin_record_num(String justin_record_num) {
    this.justin_record_num = justin_record_num;
  }
}