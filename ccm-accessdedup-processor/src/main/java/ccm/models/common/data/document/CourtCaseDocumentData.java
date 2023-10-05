package ccm.models.common.data.document;

import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CourtCaseDocumentData extends BaseDocumentData {
  private List<String> rcc_ids;
  private String part_id;
  private String participant_name;
  private String generation_date;
  private String mdoc_justin_no;
  private String court_file_no;
  private String court_location;
  private String filtered_yn;


  public CourtCaseDocumentData() {
  }

  public CourtCaseDocumentData(String event_id, String create_date, ReportDocument rd) {
    super(event_id, create_date, rd);
    setPart_id(rd.getPart_id());
    setParticipant_name(rd.getParticipant_name());
    setGeneration_date(rd.getGeneration_date());
    setMdoc_justin_no(rd.getMdoc_justin_no());
    setCourt_file_no(rd.getCourt_file_no());
    setCourt_location(rd.getCourt_location());
    setFiltered_yn(rd.getFiltered_yn());

    if(rd.getRcc_ids() != null) {
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        System.out.println(rd.getRcc_ids());
        String[] rcc_id_list = objectMapper.readValue(rd.getRcc_ids(), String[].class);
        this.setRcc_ids(Arrays.asList(rcc_id_list));
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  public List<String> getRcc_ids() {
    return rcc_ids;
  }

  public void setRcc_ids(List<String> rcc_ids) {
    this.rcc_ids = rcc_ids;
  }

  public String getPart_id() {
    return part_id;
  }

  public void setPart_id(String part_id) {
    this.part_id = part_id;
  }

  public String getParticipant_name() {
    return participant_name;
  }

  public void setParticipant_name(String participant_name) {
    this.participant_name = participant_name;
  }

  public String getGeneration_date() {
    return generation_date;
  }

  public void setGeneration_date(String generation_date) {
    this.generation_date = generation_date;
  }

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }

  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }

  public String getCourt_file_no() {
    return court_file_no;
  }

  public void setCourt_file_no(String court_file_no) {
    this.court_file_no = court_file_no;
  }

  public String getCourt_location() {
    return court_location;
  }

  public void setCourt_location(String court_location) {
    this.court_location = court_location;
  }

  public String getFiltered_yn() {
    return filtered_yn;
  }

  public void setFiltered_yn(String filtered_yn) {
    this.filtered_yn = filtered_yn;
  }

}
  