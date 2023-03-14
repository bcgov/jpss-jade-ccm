package ccm.models.common.data.document;

import java.util.List;
import ccm.models.system.justin.JustinDocumentList;
import ccm.models.system.justin.JustinDocument;

public class AllDocumentData extends BaseDocumentData {
  private String rcc_id;
  private String submit_date;
  private String agency_file_no;
  private String part_id;
  private String participant_name;
  private String witness_name;
  private String pin_number;
  private String witness_yn;
  private String expert_yn;
  private String police_officer_yn;
  private String victim_yn;
  private String filed_by;
  private String mdoc_justin_no;
  private String sworn_date;
  private String form_type_cd;
  private String form_type_description;
  private String court_file_no;
  private String document_id;
  private String court_services_form_no;
  private String issue_date;
  private String court_location;
  private String generation_date;
  private List<String> rcc_ids;
  private String exclude_youth;
  private String include_525_512_3;
  private String include_810;
  private String include_mva;
  private String include_ncr;
  private String include_non_disclosure;
  private String include_unknown_statutes;


  public AllDocumentData() {
  }

  public AllDocumentData(JustinDocumentList jdl) {
    super("", jdl);
    if(jdl.getDocuments() != null && !jdl.getDocuments().isEmpty()) {
      JustinDocument jd = jdl.getDocuments().get(0);
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

  public String getPart_id() {
    return part_id;
  }

  public void setPart_id(String part_id) {
    this.part_id = part_id;
  }

  public String getParticipant_name() {
    return participant_name;
  }

  public void setParticipant_name(String participant_name) {
    this.participant_name = participant_name;
  }

  public String getWitness_name() {
    return witness_name;
  }

  public void setWitness_name(String witness_name) {
    this.witness_name = witness_name;
  }

  public String getPin_number() {
    return pin_number;
  }

  public void setPin_number(String pin_number) {
    this.pin_number = pin_number;
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

  public String getForm_type_cd() {
    return form_type_cd;
  }

  public void setForm_type_cd(String form_type_cd) {
    this.form_type_cd = form_type_cd;
  }

  public String getForm_type_description() {
    return form_type_description;
  }

  public void setForm_type_description(String form_type_description) {
    this.form_type_description = form_type_description;
  }

  public String getCourt_file_no() {
    return court_file_no;
  }

  public void setCourt_file_no(String court_file_no) {
    this.court_file_no = court_file_no;
  }

  public String getDocument_id() {
    return document_id;
  }

  public void setDocument_id(String document_id) {
    this.document_id = document_id;
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

  public String getCourt_location() {
    return court_location;
  }

  public void setCourt_location(String court_location) {
    this.court_location = court_location;
  }

  public String getGeneration_date() {
    return generation_date;
  }

  public void setGeneration_date(String generation_date) {
    this.generation_date = generation_date;
  }

  public List<String> getRcc_ids() {
    return rcc_ids;
  }

  public void setRcc_ids(List<String> rcc_ids) {
    this.rcc_ids = rcc_ids;
  }

  public String getExclude_youth() {
    return exclude_youth;
  }

  public void setExclude_youth(String exclude_youth) {
    this.exclude_youth = exclude_youth;
  }

  public String getInclude_525_512_3() {
    return include_525_512_3;
  }

  public void setInclude_525_512_3(String include_525_512_3) {
    this.include_525_512_3 = include_525_512_3;
  }

  public String getInclude_810() {
    return include_810;
  }

  public void setInclude_810(String include_810) {
    this.include_810 = include_810;
  }

  public String getInclude_mva() {
    return include_mva;
  }

  public void setInclude_mva(String include_mva) {
    this.include_mva = include_mva;
  }

  public String getInclude_ncr() {
    return include_ncr;
  }

  public void setInclude_ncr(String include_ncr) {
    this.include_ncr = include_ncr;
  }

  public String getInclude_non_disclosure() {
    return include_non_disclosure;
  }

  public void setInclude_non_disclosure(String include_non_disclosure) {
    this.include_non_disclosure = include_non_disclosure;
  }

  public String getInclude_unknown_statutes() {
    return include_unknown_statutes;
  }

  public void setInclude_unknown_statutes(String include_unknown_statutes) {
    this.include_unknown_statutes = include_unknown_statutes;
  }

}
  