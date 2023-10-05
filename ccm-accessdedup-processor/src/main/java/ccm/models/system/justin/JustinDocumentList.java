package ccm.models.system.justin;

import java.util.List;


public class JustinDocumentList {
  private String create_date;
  private String version;
  private List<JustinDocument> documents;

  public List<JustinDocument> getDocuments() {
    return documents;
  }

  public void setDocuments(List<JustinDocument> documents) {
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
