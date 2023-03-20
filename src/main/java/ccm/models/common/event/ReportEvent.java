package ccm.models.common.event;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;

public class ReportEvent extends BaseEvent{
    private int justin_event_message_id;
    private String justin_message_event_type_cd;
    private String justin_event_dtm;
    private String justin_fetched_date;
    private String justin_guid;
    private String justin_rcc_id;
    private String generation_date;
    private String guid;
    private String mdoc_justin_no;
    private String report_name;
    private String report_type;
    private String report_url;
    private String participant_name;
    private String part_id;
    private String court_services_form_no;
    private String filtered_yn;
    private List<String> rcc_ids;


    public static final String JUSTIN_FETCHED_DATE = "FETCHED_DATE";
    public static final String JUSTIN_GUID = "GUID";
    public static final String JUSTIN_RCC_ID = "RCC_ID";  
    public static final String GENERATION_DATE = "GENERATION_DATE";
    public static final String MDOC_JUSTIN_NO = "MDOC_JUSTIN_NO";
    public static final String PARTICIPANT_NAME = "PARTICIPANT_NAME";
    public static final String PART_ID = "PART_ID";
    public static final String REPORT_NAME = "REPORT_NAME";
    public static final String REPORT_TYPE = "REPORT_TYPE";
    public static final String REPORT_URL = "REPORT_URL";
    public static final String COURT_SERVICE_FORM = "COURT_SERVICES_FORM_NO";
    public static final String FILTERED_YN = "FILTERED_YN";
    public static final String RCC_IDS = "RCC_IDS";
  
    public enum SOURCE {
      JUSTIN,
      JADE_CCM
    }
    
    public enum STATUS {
      REPORT;
    }

    public enum REPORT_TYPES {
      NARRATIVE("NARRATIVE"),
      WITNESS_STATEMENT("WITNESS_STATEMENT"),
      CPIC("CPIC-CR"),
      VEHICLE("VEHICLE"),
      DV_IPV_RISK("BC DV / IPV RISK SUMMARY"),
      DV_ATTACHMENT("BC DV / IPV RISK SUMMARY"),
      DM_ATTACHMENT("DIGITAL MEDIA RETENTION"),
      SUPPLEMENTAL("SUPPLEMENTAL"),
      SYNOPSIS("SYNOPSIS"),

      RECORD_OF_PROCEEDINGS("RECORD OF PROCEEDINGS"),
      CONVICTION_LIST("CONVICTION LIST-DEFAULT"),

      CLIENT_HISTORY_REPORT_DISPOSITION("CLIENT HISTORY REPORT - DISPOSITION AND REPORTS"),
      CLIENT_HISTORY_REPORT_FULL("CLIENT HISTORY REPORT - FULL"),
      FILE_SUMMARY_REPORT("FILE SUMMARY REPORT"),
      ACCUSED_HISTORY_REPORT("ACCUSED HISTORY REPORT");
/*
NARRATIVE
WITNESS_STATEMENT
CPIC_DOC
CPIC
VEHICLE
DV_IPV_RISK
DV_ATTACHMENT
DM_ATTACHMENT
SUPPLEMENTAL
SYNOPSIS

mdoc_justin_no + part_id
RECORD_OF_PROCEEDINGS

part_id + rcc_ids
CONVICTION_LIST

part_id + rcc_ids
CLIENT_HISTORY_REPORT_DISPOSITION
CLIENT_HISTORY_REPORT_FULL
FILE_SUMMARY_REPORT
ACCUSED_HISTORY_REPORT

*/
      private String label;

      private REPORT_TYPES(String label) {
          this.label = label;
      }

      public String getLabel() {
          return label;
      }

    }
  
    public ReportEvent() {
      super();
    }
  
    public ReportEvent(JustinEvent je) {
      this();
  
      setEvent_source(SOURCE.JUSTIN.toString());
  
      setJustin_event_message_id(je.getEvent_message_id());
      setJustin_message_event_type_cd(je.getMessage_event_type_cd());
      setJustin_event_dtm(je.getEvent_dtm());

      if(je.getMessage_event_type_cd().equals("REPORT")) {

        setEvent_status(je.getMessage_event_type_cd());
        if (je.getEvent_data() != null) {
          for( JustinEventDataElement dataElement : je.getEvent_data()) {
            switch(dataElement.getData_element_nm()){
              case GENERATION_DATE:
              this.setGeneration_date(dataElement.getData_value_txt());
              break;
              case JUSTIN_GUID:
              this.setGuid(dataElement.getData_value_txt());
              break;
              case MDOC_JUSTIN_NO:
              this.setMdoc_justin_no(dataElement.getData_value_txt());
              break;
              case PARTICIPANT_NAME:
              this.setParticipant_name(dataElement.getData_value_txt());
              break;
              case PART_ID:
              this.setPart_id(dataElement.getData_value_txt());
              break;
              case COURT_SERVICE_FORM:
              this.setCourt_services_form_no(dataElement.getData_value_txt());
              break;
              case FILTERED_YN:
              this.setFiltered_yn(dataElement.getData_value_txt());
              break;
              case RCC_IDS:
              ObjectMapper objectMapper = new ObjectMapper();
              try {
                String[] rcc_id_list = objectMapper.readValue(dataElement.getData_value_txt(), String[].class);
                this.setRcc_ids(Arrays.asList(rcc_id_list));
              } catch(Exception e) {
                e.printStackTrace();
              }
              break;
              case REPORT_NAME :
              this.setReport_name(dataElement.getData_value_txt());
              break;
              case REPORT_TYPE:
              this.setReport_type(dataElement.getData_value_txt());
              break;
              case REPORT_URL:
              this.setReport_url(dataElement.getData_value_txt());
              break;
            }
          }
        }
      } else {
          // unknown status
          setEvent_status("");
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
  
      if(getJustin_rcc_id() != null) {
        setEvent_key(getJustin_rcc_id());
      } else if(getMdoc_justin_no() != null) {
        setEvent_key(getMdoc_justin_no());
      }
    }
  
    public ReportEvent(SOURCE source, ReportEvent another) {
      super(source.name(), another);
  
      this.justin_event_message_id = another.justin_event_message_id;
      this.justin_message_event_type_cd = another.justin_message_event_type_cd;
      this.justin_event_dtm = another.justin_event_dtm;
      this.justin_fetched_date = another.justin_fetched_date;
      this.justin_guid = another.justin_guid;
      this.justin_rcc_id = another.justin_rcc_id;
    }
  
    public ReportEvent(SOURCE source, CaseUserEvent another) {
      super(source.name(), another);
  
      this.justin_event_message_id = another.getJustin_event_message_id();
      this.justin_message_event_type_cd = another.getJustin_message_event_type_cd();
      this.justin_event_dtm = another.getJustin_event_dtm();
      this.justin_fetched_date = another.getJustin_fetched_date();
      this.justin_guid = another.getJustin_guid();
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

    public String getGeneration_date() {
      return generation_date;
    }

    public void setGeneration_date(String generation_date) {
      this.generation_date = generation_date;
    }

    public String getGuid() {
      return guid;
    }

    public void setGuid(String guid) {
      this.guid = guid;
    }

    public String getMdoc_justin_no() {
      return mdoc_justin_no;
    }

    public void setMdoc_justin_no(String mdoc_justin_no) {
      this.mdoc_justin_no = mdoc_justin_no;
    }

    public String getReport_name() {
      return report_name;
    }

    public void setReport_name(String report_name) {
      this.report_name = report_name;
    }

    public String getReport_type() {
      return report_type;
    }

    public void setReport_type(String report_type) {
      this.report_type = report_type;
    }

    public String getReport_url() {
      return report_url;
    }

    public void setReport_url(String report_url) {
      this.report_url = report_url;
    }

    public String getParticipant_name() {
      return participant_name;
    }

    public void setParticipant_name(String participant_name) {
      this.participant_name = participant_name;
    }

    public String getPart_id() {
      return part_id;
    }

    public void setPart_id(String part_id) {
      this.part_id = part_id;
    }

    public String getCourt_services_form_no() {
      return court_services_form_no;
    }

    public void setCourt_services_form_no(String court_services_form_no) {
      this.court_services_form_no = court_services_form_no;
    }

    public String getFiltered_yn() {
      return filtered_yn;
    }

    public void setFiltered_yn(String filtered_yn) {
      this.filtered_yn = filtered_yn;
    }

    public List<String> getRcc_ids() {
      return rcc_ids;
    }

    public void setRcc_ids(List<String> rcc_ids) {
      this.rcc_ids = rcc_ids;
    }


}