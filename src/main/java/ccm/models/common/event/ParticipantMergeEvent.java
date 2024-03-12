package ccm.models.common.event;

import java.util.Iterator;
import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;

public class ParticipantMergeEvent extends BaseEvent {
  private int justin_event_message_id;
  private String justin_message_event_type_cd;
  private String justin_event_dtm;
  private String justin_from_part_id;
  private String justin_guid;
  private String justin_to_part_id;
  private String justin_fetched_date;

  public static final String JUSTIN_FETCHED_DATE = "FETCHED_DATE";
  public static final String JUSTIN_GUID = "GUID";
  public static final String JUSTIN_FROM_PART_ID = "FROM_PART_ID";  
  public static final String JUSTIN_TO_PART_ID = "TO_PART_ID";

  public enum SOURCE {
    JUSTIN,
    ISL_CCM
  }
  
  public enum STATUS {
    PART_MERGE;
  }

  public ParticipantMergeEvent() {
    super();
  }

  public ParticipantMergeEvent(JustinEvent je) {
    this();

    setEvent_source(SOURCE.JUSTIN.toString());

    setJustin_event_message_id(je.getEvent_message_id());
    setJustin_message_event_type_cd(je.getMessage_event_type_cd());
    setJustin_event_dtm(je.getEvent_dtm());

    switch(JustinEvent.STATUS.valueOf(je.getMessage_event_type_cd())) {
      case PART_MERGE:
        setEvent_status(STATUS.PART_MERGE.toString());
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
        case JUSTIN_FROM_PART_ID:
          setJustin_from_part_id(jed.getData_value_txt());
          break;
        case JUSTIN_TO_PART_ID:
          setJustin_to_part_id(jed.getData_value_txt());
          break;
      }
    }

    setEvent_key(getJustin_from_part_id().concat(",").concat(getJustin_to_part_id()));
  }

  public ParticipantMergeEvent(SOURCE source, ParticipantMergeEvent another) {
    super(source.name(), another);

    this.justin_event_message_id = another.justin_event_message_id;
    this.justin_message_event_type_cd = another.justin_message_event_type_cd;
    this.justin_event_dtm = another.justin_event_dtm;
    this.justin_fetched_date = another.justin_fetched_date;
    this.justin_guid = another.justin_guid;
    this.justin_from_part_id = another.justin_from_part_id;
    this.justin_to_part_id = another.justin_to_part_id;
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

  public String getJustin_from_part_id() {
    return justin_from_part_id;
  }

  public void setJustin_from_part_id(String justin_from_part_id) {
    this.justin_from_part_id = justin_from_part_id;
  }

  public String getJustin_to_part_id() {
    return justin_to_part_id;
  }

  public void setJustin_to_part_id(String justin_to_part_id) {
    this.justin_to_part_id = justin_to_part_id;
  }
}