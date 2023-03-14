package ccm.models.common.data.document;

import ccm.models.system.justin.JustinDocumentList;
import ccm.models.system.justin.JustinDocument;

public class NarrativeDocumentData extends BaseDocumentData {
  private String rcc_id;
  private String submit_date;
  private String agency_file_no;

  public NarrativeDocumentData() {
  }

  public NarrativeDocumentData(String event_id, JustinDocumentList jdl) {
    super(event_id, jdl);
    if(jdl.getDocuments() != null && !jdl.getDocuments().isEmpty()) {
      JustinDocument jd = jdl.getDocuments().get(0);
      setRcc_id(jd.getRcc_id());
      setSubmit_date(jd.getSubmit_date());
      setAgency_file_no(jd.getAgency_file_no());
    }
  }

  public String getRcc_id() {
    return rcc_id;
  }

  public void setRcc_id(String rcc_id) {
    this.rcc_id = rcc_id;
  }

  public String getSubmit_date() {
    return submit_date;
  }

  public void setSubmit_date(String submit_date) {
    this.submit_date = submit_date;
  }

  public String getAgency_file_no() {
    return agency_file_no;
  }

  public void setAgency_file_no(String agency_file_no) {
    this.agency_file_no = agency_file_no;
  }

}
  