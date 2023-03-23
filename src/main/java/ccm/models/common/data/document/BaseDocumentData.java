package ccm.models.common.data.document;

import ccm.models.system.justin.JustinDocumentList;
import ccm.models.system.justin.JustinDocument;

public class BaseDocumentData {
  private String create_date;
  private String report_type;
  private String report_format;
  private String document_type;
  private String location;

  private String data;

  public BaseDocumentData() {
  }

  public BaseDocumentData(String event_id, String create_date, JustinDocument jd) {
    setCreate_date(create_date);
    setReport_type(jd.getReport_type());
    setReport_format(jd.getReport_format());
    setDocument_type(jd.getDocument_type());
    setLocation(event_id);
    setData(jd.getData());
  }

  public String getCreate_date() {
    return create_date;
  }

  public void setCreate_date(String create_date) {
    this.create_date = create_date;
  }

  public String getReport_type() {
    return report_type;
  }

  public void setReport_type(String report_type) {
    this.report_type = report_type;
  }

  public String getReport_format() {
    return report_format;
  }

  public void setReport_format(String report_format) {
    this.report_format = report_format;
  }

  public String getDocument_type() {
    return document_type;
  }

  public void setDocument_type(String document_type) {
    this.document_type = document_type;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }


}
  