package ccm.models.common.event;

import java.util.Iterator;

import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;

public class FileNoteEvent extends BaseEvent{
    private int justin_event_message_id;
  private String justin_message_event_type_cd;
  private String justin_event_dtm;
  private String justin_fetched_date;
  private String justin_guid;
  private String justin_mdoc_no;
  private String file_note_id;

  public static final String GUID = "GUID";
  public static final String FILE_NOTE_ID = "FILE_NOTE_ID";
  public static final String FETCHED_DATE = "FETCHED_DATE";
  public enum SOURCE {
    JUSTIN;
  }

  public enum STATUS {
    FILE_NOTE
  }

  public FileNoteEvent(JustinEvent je) {
    this();
    setEvent_source(SOURCE.JUSTIN.toString());

    setJustin_event_message_id(je.getEvent_message_id());
    setJustin_message_event_type_cd(je.getMessage_event_type_cd());
    setJustin_event_dtm(je.getEvent_dtm());

    switch(JustinEvent.STATUS.valueOf(je.getMessage_event_type_cd())) {
      
      case FILE_NOTE:
        setEvent_status(STATUS.FILE_NOTE.toString());
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
        case FILE_NOTE_ID:
          setFile_note_id(jed.getData_value_txt());
          break;
        case GUID:
          setJustin_guid(jed.getData_value_txt());
          break;
        case FETCHED_DATE:
          setJustin_fetched_date(jed.getData_value_txt());
          break;
      }
    }

    setEvent_key(getFile_note_id());
  }

  public FileNoteEvent() {
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

  public String getFile_note_id() {
    return file_note_id;
  }

  public void setFile_note_id(String file_note_id) {
    this.file_note_id = file_note_id;
  }
}
