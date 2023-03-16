package ccm.models.common.data.document;

import ccm.models.system.justin.JustinDocumentList;
import ccm.models.system.justin.JustinDocument;

public class ChargeAssessmentDocumentData extends BaseDocumentData {
  private String rcc_id;
  private String submit_date;
  private String agency_file_no;
  private String witness_name;
  private String witness_yn;
  private String expert_yn;
  private String police_officer_yn;
  private String victim_yn;
  private String filed_by;
  private String participant_name;
  private String officer_pin_number;


  public ChargeAssessmentDocumentData() {
  }

  public ChargeAssessmentDocumentData(String event_id, JustinDocumentList jdl) {
    super(event_id, jdl);
    if(jdl.getDocuments() != null && !jdl.getDocuments().isEmpty()) {
      JustinDocument jd = jdl.getDocuments().get(0);
      setRcc_id(jd.getRcc_id());
      setSubmit_date(jd.getSubmit_date());
      setAgency_file_no(jd.getAgency_file_no());
      setWitness_name(jd.getWitness_name());
      setWitness_yn(jd.getWitness_yn());
      setExpert_yn(jd.getExpert_yn());
      setPolice_officer_yn(jd.getPolice_officer_yn());
      setVictim_yn(jd.getVictim_yn());
      setFiled_by(jd.getFiled_by());
      setParticipant_name(jd.getParticipant_name());
      setOfficer_pin_number(jd.getOfficer_pin_number());
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

  public String getWitness_name() {
    return witness_name;
  }

  public void setWitness_name(String witness_name) {
    this.witness_name = witness_name;
  }

  public String getWitness_yn() {
    return witness_yn;
  }

  public void setWitness_yn(String witness_yn) {
    this.witness_yn = witness_yn;
  }

  public String getExpert_yn() {
    return expert_yn;
  }

  public void setExpert_yn(String expert_yn) {
    this.expert_yn = expert_yn;
  }

  public String getPolice_officer_yn() {
    return police_officer_yn;
  }

  public void setPolice_officer_yn(String police_officer_yn) {
    this.police_officer_yn = police_officer_yn;
  }

  public String getVictim_yn() {
    return victim_yn;
  }

  public void setVictim_yn(String victim_yn) {
    this.victim_yn = victim_yn;
  }

  public String getFiled_by() {
    return filed_by;
  }

  public void setFiled_by(String filed_by) {
    this.filed_by = filed_by;
  }

  public String getParticipant_name() {
    return participant_name;
  }

  public void setParticipant_name(String participant_name) {
    this.participant_name = participant_name;
  }

  public String getOfficer_pin_number() {
    return officer_pin_number;
  }

  public void setOfficer_pin_number(String officer_pin_number) {
    this.officer_pin_number = officer_pin_number;
  }

}
  