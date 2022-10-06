package ccm.models.system.justin;

import java.util.List;

public class JustinAgencyFile {
  private String rcc_id;
  private String agency_file_no;
  private String security_clearance_level;
  private String synopsis;
  private String initiating_agency_name;
  private String initiating_agency_identifier;
  private String investigating_officer_name;
  private String investigating_officer_pin;
  private String rcc_submit_date;
  private String kfile_yn;
  private String vul1;
  private String chi1;
  private String crn_decision_agency_identifier;
  private String crn_decision_agency_name;
  private String assessment_crown_name;
  private String assessment_crown_part_id;
  private String case_decision_cd;

  private String note_to_file;

  private String charge;
  private String limitation_date;
  private String min_offence_date;
  private List<JustinAccused> accused;

  private String primary_yn;

  public String getAgency_file_no() {
    return agency_file_no;
  }

  public void setAgency_file_no(String agency_file_no) {
    this.agency_file_no = agency_file_no;
  }

  public String getSecurity_clearance_level() {
    return security_clearance_level;
  }

  public void setSecurity_clearance_level(String security_clearance_level) {
    this.security_clearance_level = security_clearance_level;
  }

  public String getSynopsis() {
    return synopsis;
  }

  public void setSynopsis(String synopsis) {
    this.synopsis = synopsis;
  }

  public String getInitiating_agency_name() {
    return initiating_agency_name;
  }

  public void setInitiating_agency_name(String initiating_agency_name) {
    this.initiating_agency_name = initiating_agency_name;
  }

  public String getInitiating_agency_identifier() {
    return initiating_agency_identifier;
  }

  public void setInitiating_agency_identifier(String initiating_agency_identifier) {
    this.initiating_agency_identifier = initiating_agency_identifier;
  }

  public String getInvestigating_officer_name() {
    return investigating_officer_name;
  }

  public void setInvestigating_officer_name(String investigating_officer_name) {
    this.investigating_officer_name = investigating_officer_name;
  }

  public String getInvestigating_officer_pin() {
    return investigating_officer_pin;
  }

  public void setInvestigating_officer_pin(String investigating_officer_pin) {
    this.investigating_officer_pin = investigating_officer_pin;
  }

  public String getRcc_submit_date() {
    return rcc_submit_date;
  }

  public void setRcc_submit_date(String rcc_submit_date) {
    this.rcc_submit_date = rcc_submit_date;
  }

  public String getKfile_yn() {
    return kfile_yn;
  }

  public void setKfile_yn(String kfile_yn) {
    this.kfile_yn = kfile_yn;
  }

  public String getVul1() {
    return vul1;
  }

  public void setVul1(String vul1) {
    this.vul1 = vul1;
  }

  public String getChi1() {
    return chi1;
  }

  public void setChi1(String chi1) {
    this.chi1 = chi1;
  }

  public String getCrn_decision_agency_identifier() {
    return crn_decision_agency_identifier;
  }

  public void setCrn_decision_agency_identifier(String crn_decision_agency_identifier) {
    this.crn_decision_agency_identifier = crn_decision_agency_identifier;
  }

  public String getCrn_decision_agency_name() {
    return crn_decision_agency_name;
  }

  public void setCrn_decision_agency_name(String crn_decision_agency_name) {
    this.crn_decision_agency_name = crn_decision_agency_name;
  }

  public String getAssessment_crown_name() {
    return assessment_crown_name;
  }

  public void setAssessment_crown_name(String assessment_crown_name) {
    this.assessment_crown_name = assessment_crown_name;
  }

  public String getAssessment_crown_part_id() {
    return assessment_crown_part_id;
  }

  public void setAssessment_crown_part_id(String assessment_crown_part_id) {
    this.assessment_crown_part_id = assessment_crown_part_id;
  }

  public String getCase_decision_cd() {
    return case_decision_cd;
  }

  public void setCase_decision_cd(String case_decision_cd) {
    this.case_decision_cd = case_decision_cd;
  }

  public String getNote_to_file() {
    return note_to_file;
  }

  public void setNote_to_file(String note_to_file) {
    this.note_to_file = note_to_file;
  }

  public String getCharge() {
    return charge;
  }

  public void setCharge(String charge) {
    this.charge = charge;
  }

  public String getLimitation_date() {
    return limitation_date;
  }

  public void setLimitation_date(String limitation_date) {
    this.limitation_date = limitation_date;
  }

  public String getMin_offence_date() {
    return min_offence_date;
  }

  public void setMin_offence_date(String min_offence_date) {
    this.min_offence_date = min_offence_date;
  }

  public String getRcc_id() {
    return this.rcc_id;
  }

  public void setRcc_id(String rcc_id) {
    this.rcc_id = rcc_id;
  }
  
  public List<JustinAccused> getAccused() {
    return accused;
  }

  public void setAccused(List<JustinAccused> accused) {
    this.accused = accused;
  }

  public String getPrimary_yn() {
    return primary_yn;
  }

  public void setPrimary_yn(String primary_yn) {
    this.primary_yn = primary_yn;
  }

}
