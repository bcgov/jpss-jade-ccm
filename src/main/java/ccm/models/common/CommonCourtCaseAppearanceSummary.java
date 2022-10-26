package ccm.models.common;

import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class CommonCourtCaseAppearanceSummary {
  private String initial_appr_dtm;
  private String initial_appr_rsn_cd;
  private String next_appr_dtm;
  private String next_appr_rsn_cd;
  private String trial_start_appr_dtm;
  private String trial_start_appr_rsn_cd;

  public CommonCourtCaseAppearanceSummary() {
  }

  public CommonCourtCaseAppearanceSummary(JustinCourtAppearanceSummary jaf) {
    setInitial_appr_dtm(jaf.getInitial_appr_dtm());
    setInitial_appr_rsn_cd(jaf.getInitial_appr_rsn_cd());
    setNext_appr_dtm(jaf.getNext_appr_dtm());
    setNext_appr_rsn_cd(jaf.getNext_appr_rsn_cd());
    setTrial_start_appr_dtm(jaf.getTrial_start_appr_dtm());
    setTrial_start_appr_rsn_cd(jaf.getTrial_start_appr_rsn_cd());
  }

  public String getInitial_appr_dtm() {
    return initial_appr_dtm;
  }
  public void setInitial_appr_dtm(String initial_appr_dt) {
    this.initial_appr_dtm = initial_appr_dt;
  }
  public String getInitial_appr_rsn_cd() {
    return initial_appr_rsn_cd;
  }
  public void setInitial_appr_rsn_cd(String initial_appr_rsn_cd) {
    this.initial_appr_rsn_cd = initial_appr_rsn_cd;
  }
  public String getNext_appr_dtm() {
    return next_appr_dtm;
  }
  public void setNext_appr_dtm(String next_appr_dtm) {
    this.next_appr_dtm = next_appr_dtm;
  }
  public String getNext_appr_rsn_cd() {
    return next_appr_rsn_cd;
  }
  public void setNext_appr_rsn_cd(String next_appr_rsn_cd) {
    this.next_appr_rsn_cd = next_appr_rsn_cd;
  }
  public String getTrial_start_appr_dtm() {
    return trial_start_appr_dtm;
  }
  public void setTrial_start_appr_dtm(String trial_start_appr_dtm) {
    this.trial_start_appr_dtm = trial_start_appr_dtm;
  }
  public String getTrial_start_appr_rsn_cd() {
    return trial_start_appr_rsn_cd;
  }
  public void setTrial_start_appr_rsn_cd(String trial_start_appr_rsn_cd) {
    this.trial_start_appr_rsn_cd = trial_start_appr_rsn_cd;
  }


}
  