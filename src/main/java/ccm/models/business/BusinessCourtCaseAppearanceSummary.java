package ccm.models.business;

import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class BusinessCourtCaseAppearanceSummary {
  private String initial_appr_dt;
  private String initial_appr_rsn_cd;
  private String next_appr_dt;
  private String next_appr_rsn_cd;
  private String trial_start_appr_dt;
  private String trial_start_appr_rsn_cd;

  public BusinessCourtCaseAppearanceSummary() {
  }

  public BusinessCourtCaseAppearanceSummary(JustinCourtAppearanceSummary jaf) {
    setInitial_appr_dt(jaf.getInitial_appr_dt());
    setInitial_appr_rsn_cd(jaf.getInitial_appr_rsn_cd());
    setNext_appr_dt(jaf.getNext_appr_dt());
    setNext_appr_rsn_cd(jaf.getNext_appr_rsn_cd());
    setTrial_start_appr_dt(jaf.getTrial_start_appr_dt());
    setTrial_start_appr_rsn_cd(jaf.getTrial_start_appr_rsn_cd());
  }

  public String getInitial_appr_dt() {
    return initial_appr_dt;
  }
  public void setInitial_appr_dt(String initial_appr_dt) {
    this.initial_appr_dt = initial_appr_dt;
  }
  public String getInitial_appr_rsn_cd() {
    return initial_appr_rsn_cd;
  }
  public void setInitial_appr_rsn_cd(String initial_appr_rsn_cd) {
    this.initial_appr_rsn_cd = initial_appr_rsn_cd;
  }
  public String getNext_appr_dt() {
    return next_appr_dt;
  }
  public void setNext_appr_dt(String next_appr_dt) {
    this.next_appr_dt = next_appr_dt;
  }
  public String getNext_appr_rsn_cd() {
    return next_appr_rsn_cd;
  }
  public void setNext_appr_rsn_cd(String next_appr_rsn_cd) {
    this.next_appr_rsn_cd = next_appr_rsn_cd;
  }
  public String getTrial_start_appr_dt() {
    return trial_start_appr_dt;
  }
  public void setTrial_start_appr_dt(String trial_start_appr_dt) {
    this.trial_start_appr_dt = trial_start_appr_dt;
  }
  public String getTrial_start_appr_rsn_cd() {
    return trial_start_appr_rsn_cd;
  }
  public void setTrial_start_appr_rsn_cd(String trial_start_appr_rsn_cd) {
    this.trial_start_appr_rsn_cd = trial_start_appr_rsn_cd;
  }


}
  