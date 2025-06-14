package ccm.models.common.data.document;

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
  private String participants;
  private String sequence_nos;


  public ChargeAssessmentDocumentData() {
  }

  public ChargeAssessmentDocumentData(String event_id, String create_date, ReportDocument rd) {
    super(event_id, create_date, rd);
    setRcc_id(rd.getRcc_id());
    setSubmit_date(rd.getSubmit_date());
    setAgency_file_no(rd.getAgency_file_no());
    setWitness_name(rd.getWitness_name());
    setWitness_yn(rd.getWitness_yn());
    setExpert_yn(rd.getExpert_yn());
    setPolice_officer_yn(rd.getPolice_officer_yn());
    setVictim_yn(rd.getVictim_yn());
    setFiled_by(rd.getFiled_by());
    setParticipant_name(rd.getParticipant_name());
    setOfficer_pin_number(rd.getOfficer_pin_number());
    setParticipants(rd.getParticipants());
    setSequence_nos(rd.getSequence_nos());
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

  public String getParticipants() {
    return participants;
  }

  public void setParticipants(String participants) {
    this.participants = participants;
  }

  public String getSequence_nos() {
    return sequence_nos;
  }

  public void setSequence_nos(String sequence_nos) {
    this.sequence_nos = sequence_nos;
  }

}
  