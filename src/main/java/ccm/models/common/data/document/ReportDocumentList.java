package ccm.models.common.data.document;

import java.util.ArrayList;
import java.util.List;

import ccm.models.system.justin.JustinDocument;
import ccm.models.system.justin.JustinDocumentList;


public class ReportDocumentList {
  private String create_date;
  private String version;
  private List<ReportDocument> documents;

  public ReportDocumentList() {
  }
  public ReportDocumentList(JustinDocumentList jdl) {
    setCreate_date(jdl.getCreate_date());
    setVersion(jdl.getVersion());
    documents = new ArrayList<ReportDocument>();
    for(JustinDocument jd : jdl.getDocuments()) {
      ReportDocument rd = new ReportDocument(jd);
      documents.add(rd);
    }
  }

  public List<ReportDocument> getDocuments() {
    return documents;
  }

  public void setDocuments(List<ReportDocument> documents) {
    this.documents = documents;
  }

  public String getCreate_date() {
    return create_date;
  }

  public void setCreate_date(String create_date) {
    this.create_date = create_date;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
