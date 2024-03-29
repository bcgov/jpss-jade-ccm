package ccm.models.system.justin;

import java.util.ArrayList;
import java.util.List;

import ccm.models.common.event.ReportEvent;

public class JustinDocumentKeyList {
  private List<JustinDocumentKey> document_keys;

  public JustinDocumentKeyList(ReportEvent re) {
    ArrayList<JustinDocumentKey> keyValues = new ArrayList<JustinDocumentKey>();
    JustinDocumentKey documentKey = new JustinDocumentKey();
    documentKey.setRcc_id(re.getJustin_rcc_id());
    documentKey.setMdoc_justin_no(re.getMdoc_justin_no());
    documentKey.setPart_id(re.getPart_id());
    documentKey.setReport_types(re.getReport_type());
    documentKey.setReport_object_url(re.getReport_url());
    documentKey.setRcc_ids(re.getRcc_ids());
    documentKey.setFiltered_yn(re.getFiltered_yn());
    documentKey.setCourt_services_form_no(re.getCourt_services_form_no());
    documentKey.setParticipant_name(re.getParticipant_name());
    documentKey.setGeneration_date(re.getGeneration_date());
    documentKey.setImage_id(re.getImage_id());
    documentKey.setDocm_id(re.getDocm_id());
    //documentKey.setFetched_date(re.getJustin_fetched_date());

    keyValues.add(documentKey);
    setDocument_keys(keyValues);
  }

  public List<JustinDocumentKey> getDocument_keys() {
    return document_keys;
  }

  public void setDocument_keys(List<JustinDocumentKey> document_keys) {
    this.document_keys = document_keys;
  }

}
