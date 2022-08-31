package ccm.models.system.justin;

import java.util.List;

public class JustinCourtFile {
  private String mdoc_justin_no;
  private String home_court_agency_name;
  private String home_court_agency_identifier;
  private String court_file_no;
  private String type_reference;
  private String mdoc_seq_no;
  private String kfile_yn;
  private String court_level_cd;
  private String court_class_cd;
  private String file_designation;
  private String sworn_date;
  private String approved_charges;
  private String crown_election;

  private String mdoc_relation_type_cd;

  private List<JustinAccused> mdocaccused;
  private List<JustinAgencyFile> related_rcc;
  private List<JustinCourtFile> related_court_file;

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }
  public String getHome_court_agency_name() {
    return home_court_agency_name;
  }
  public void setHome_court_agency_name(String home_court_agency_name) {
    this.home_court_agency_name = home_court_agency_name;
  }
  public String getHome_court_agency_identifier() {
    return home_court_agency_identifier;
  }
  public void setHome_court_agency_identifier(String home_court_agency_identifier) {
    this.home_court_agency_identifier = home_court_agency_identifier;
  }
  public String getCourt_file_no() {
    return court_file_no;
  }
  public void setCourt_file_no(String court_file_no) {
    this.court_file_no = court_file_no;
  }
  public String getType_reference() {
    return type_reference;
  }
  public void setType_reference(String type_reference) {
    this.type_reference = type_reference;
  }
  public String getMdoc_seq_no() {
    return mdoc_seq_no;
  }
  public void setMdoc_seq_no(String mdoc_seq_no) {
    this.mdoc_seq_no = mdoc_seq_no;
  }
  public String getKfile_yn() {
    return kfile_yn;
  }
  public void setKfile_yn(String kfile_yn) {
    this.kfile_yn = kfile_yn;
  }
  public String getCourt_level_cd() {
    return court_level_cd;
  }
  public void setCourt_level_cd(String court_level_cd) {
    this.court_level_cd = court_level_cd;
  }
  public String getCourt_class_cd() {
    return court_class_cd;
  }
  public void setCourt_class_cd(String court_class_cd) {
    this.court_class_cd = court_class_cd;
  }
  public String getFile_designation() {
    return file_designation;
  }
  public void setFile_designation(String file_designation) {
    this.file_designation = file_designation;
  }
  public String getSworn_date() {
    return sworn_date;
  }
  public void setSworn_date(String sworn_date) {
    this.sworn_date = sworn_date;
  }
  public String getApproved_charges() {
    return approved_charges;
  }
  public void setApproved_charges(String approved_charges) {
    this.approved_charges = approved_charges;
  }
  public String getCrown_election() {
    return crown_election;
  }
  public void setCrown_election(String crown_election) {
    this.crown_election = crown_election;
  }
  public String getMdoc_relation_type_cd() {
    return mdoc_relation_type_cd;
  }
  public void setMdoc_relation_type_cd(String mdoc_relation_type_cd) {
    this.mdoc_relation_type_cd = mdoc_relation_type_cd;
  }
  public List<JustinAccused> getMdocaccused() {
    return mdocaccused;
  }
  public void setMdocaccused(List<JustinAccused> mdocaccused) {
    this.mdocaccused = mdocaccused;
  }
  public List<JustinAgencyFile> getRelated_rcc() {
    return related_rcc;
  }
  public void setRelated_rcc(List<JustinAgencyFile> related_rcc) {
    this.related_rcc = related_rcc;
  }
  public List<JustinCourtFile> getRelated_court_file() {
    return related_court_file;
  }
  public void setRelated_court_file(List<JustinCourtFile> related_court_file) {
    this.related_court_file = related_court_file;
  }

}
  