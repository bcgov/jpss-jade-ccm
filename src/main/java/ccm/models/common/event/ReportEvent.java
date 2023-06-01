package ccm.models.common.event;

import java.util.Iterator;

import ccm.models.system.justin.JustinEvent;
import ccm.models.system.justin.JustinEventDataElement;
import ccm.utils.DateTimeUtils;

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
    private String rcc_ids;
    private String image_id;
    private String docm_id;

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
    public static final String IMAGE_ID = "IMAGE_ID";
    public static final String DOCM_ID = "DOCM_ID";
  
    public enum SOURCE {
      JUSTIN,
      JADE_CCM
    }
    
    public enum STATUS {
      REPORT,
      DOCM,
      INFO_DOCM;
    }

    public enum REPORT_TYPES {
      NARRATIVE("NAR", "NARRATIVE"),
      WITNESS_STATEMENT("STMT", "STATEMENT"),
      CPIC("CPIC-CR", "CPIC-CR"),
      VEHICLE("VEH", "VEHICLES"),
      DV_IPV_RISK("IPVRISK", "BC DV / IPV RISK SUMMARY"),
      DV_ATTACHMENT("DVRISK", "BC DV / IPV RISK SUMMARY"),
      DM_ATTACHMENT("MR", "DIGITAL MEDIA RETENTION"),
      SUPPLEMENTAL("SUPP", "SUPPLEMENTAL"),
      SYNOPSIS("SYN", "SYNOPSIS"),
      ACCUSED_INFO("ACCUSED_INFO", "ACCUSED INFORMATION"),

      RECORD_OF_PROCEEDINGS("ROP", "RECORD OF PROCEEDINGS"),
      CONVICTION_LIST("CL", "CONVICTION LIST"),

      CLIENT_HISTORY_REPORT_DISPOSITION("CORNET-DISPO", "CLIENT HISTORY REPORT - DISPOSITION AND REPORTS"),
      CLIENT_HISTORY_REPORT_FULL("CORNET-FULL", "CLIENT HISTORY REPORT - FULL"),
      FILE_SUMMARY_REPORT("FSR", "FILE SUMMARY"),
      ACCUSED_HISTORY_REPORT("AHR", "ACCUSED HISTORY REPORT"),
      RELEASE_ORDER("RO", "RELEASE ORDER"),

      INFORMATION("INFO", "SWORN INFORMATION"),
      DOCUMENT("DOCUMENT", "DOCUMENT"),
      RELEASE_DOCUMENT("RO", "APPEARANCE NOTICE"),
      SENTENCE_DOCUMENT("SENTENCE_DOCUMENT", "CONDITIONAL SENTENCE ORDER");

      private String label;
      private String description;

      private REPORT_TYPES(String label, String description) {
          this.label = label;
          this.description = description;
      }

      public String getLabel() {
          return label;
      }

      public String getDescription() {
        return description;
      }

      public static REPORT_TYPES fromString(String text) {
        for (REPORT_TYPES b : REPORT_TYPES.values()) {
            if (b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
      }
    }
  
    public ReportEvent() {
      super();
      setJustin_event_dtm(DateTimeUtils.generateCurrentDtm());
    }
  
    public ReportEvent(JustinEvent je) {
      this();
  
      setEvent_source(SOURCE.JUSTIN.toString());
  
      setJustin_event_message_id(je.getEvent_message_id());
      setJustin_message_event_type_cd(je.getMessage_event_type_cd());
      setJustin_event_dtm(je.getEvent_dtm());

      if(je.getMessage_event_type_cd().equals(STATUS.REPORT.name()) ||
         je.getMessage_event_type_cd().equals(STATUS.DOCM.name()) ||
         je.getMessage_event_type_cd().equals(STATUS.INFO_DOCM.name())) {

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
                this.setRcc_ids(dataElement.getData_value_txt());
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
              case IMAGE_ID:
                this.setImage_id(dataElement.getData_value_txt());
                break;
              case DOCM_ID:
                this.setDocm_id(dataElement.getData_value_txt());
                break;
            }
          }
        }
      } else {
          // unknown status
          setEvent_status("");
      }
      if(je.getMessage_event_type_cd().equals(STATUS.DOCM.name())) {
        this.setReport_type("DOCUMENT");
      } else if(je.getMessage_event_type_cd().equals(STATUS.INFO_DOCM.name())) {
        this.setReport_type("INFORMATION");
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

    public String getRcc_ids() {
      return rcc_ids;
    }

    public void setRcc_ids(String rcc_ids) {
      this.rcc_ids = rcc_ids;
    }

    public String getImage_id() {
      return image_id;
    }

    public void setImage_id(String image_id) {
      this.image_id = image_id;
    }

    public String getDocm_id() {
      return docm_id;
    }

    public void setDocm_id(String docm_id) {
      this.docm_id = docm_id;
    }

}