package ccm.models.common.data.document;

import java.util.List;
import ccm.models.system.justin.JustinDocumentList;
import ccm.models.system.justin.JustinDocument;

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

  public CourtCaseDocumentData(String event_id, JustinDocumentList jdl) {
    super(event_id, jdl);
    if(jdl.getDocuments() != null && !jdl.getDocuments().isEmpty()) {
      JustinDocument jd = jdl.getDocuments().get(0);
      setRcc_ids(jd.getRcc_ids());
      setPart_id(jd.getPart_id());
      setParticipant_name(jd.getParticipant_name());
      setGeneration_date(jd.getGeneration_date());
      setMdoc_justin_no(jd.getMdoc_justin_no());
      setCourt_file_no(jd.getCourt_file_no());
      setCourt_location(jd.getCourt_location());
      setFiltered_yn(jd.getFiltered_yn());
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
  