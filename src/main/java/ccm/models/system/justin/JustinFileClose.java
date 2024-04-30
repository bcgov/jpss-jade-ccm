package ccm.models.system.justin;

import ccm.models.common.FileCloseData;

public class JustinFileClose {
    
private String mdoc_justin_no;
private String home_court_agency_name;
private String court_file_no;
private String type_reference;
private String mdoc_seq_no;
private String kfile_yn;
private String court_level_cd;
private FileCloseData file_close_data ;



  public FileCloseData getFile_close_data() {
    return file_close_data;
}
public void setFile_close_data(FileCloseData file_close_data) {
    this.file_close_data = file_close_data;
}
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
}
