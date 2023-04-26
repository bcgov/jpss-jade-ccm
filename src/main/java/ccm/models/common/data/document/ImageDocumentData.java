package ccm.models.common.data.document;

public class ImageDocumentData extends BaseDocumentData {
  private String mdoc_justin_no;
  private String part_id;
  private String sworn_date;
  private String form_type_description;
  private String form_type_code;
  private String court_file_number;
  private String primary_rcc_id;
  private String image_id;
  private String participant_name;
  private String court_services_form_no;
  private String issue_date;


  public ImageDocumentData() {
  }

  public ImageDocumentData(String event_id, String create_date, ReportDocument rd) {
    super(event_id, create_date, rd);
    setMdoc_justin_no(rd.getMdoc_justin_no());
    setPart_id(rd.getPart_id());
    setSworn_date(rd.getSworn_date());
    setForm_type_description(rd.getForm_type_description());
    setForm_type_code(rd.getForm_type_code());
    setCourt_file_number(rd.getCourt_file_number());
    setPrimary_rcc_id(rd.getPrimary_rcc_id());
    setImage_id(rd.getImage_id());
    setParticipant_name(rd.getParticipant_name());
    setCourt_services_form_no(rd.getCourt_services_form_no());
    setIssue_date(rd.getIssue_date());

  }

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }

  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }

  public String getSworn_date() {
    return sworn_date;
  }

  public void setSworn_date(String sworn_date) {
    this.sworn_date = sworn_date;
  }

  public String getForm_type_description() {
    return form_type_description;
  }

  public void setForm_type_description(String form_type_description) {
    this.form_type_description = form_type_description;
  }

  public String getForm_type_code() {
    return form_type_code;
  }

  public void setForm_type_code(String form_type_code) {
    this.form_type_code = form_type_code;
  }

  public String getCourt_file_number() {
    return court_file_number;
  }

  public void setCourt_file_number(String court_file_number) {
    this.court_file_number = court_file_number;
  }

  public String getPrimary_rcc_id() {
    return primary_rcc_id;
  }

  public void setPrimary_rcc_id(String primary_rcc_id) {
    this.primary_rcc_id = primary_rcc_id;
  }

  public String getPart_id() {
    return part_id;
  }

  public void setPart_id(String part_id) {
    this.part_id = part_id;
  }

  public String getImage_id() {
    return image_id;
  }

  public void setImage_id(String image_id) {
    this.image_id = image_id;
  }

  public String getParticipant_name() {
    return participant_name;
  }

  public void setParticipant_name(String participant_name) {
    this.participant_name = participant_name;
  }

  public String getCourt_services_form_no() {
    return court_services_form_no;
  }

  public void setCourt_services_form_no(String court_services_form_no) {
    this.court_services_form_no = court_services_form_no;
  }

  public String getIssue_date() {
    return issue_date;
  }

  public void setIssue_date(String issue_date) {
    this.issue_date = issue_date;
  }

}
  