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
    documentKey.setPart_id(null);
    documentKey.setReport_types(re.getReport_type());
    documentKey.setReport_object_url(re.getReport_url());

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
