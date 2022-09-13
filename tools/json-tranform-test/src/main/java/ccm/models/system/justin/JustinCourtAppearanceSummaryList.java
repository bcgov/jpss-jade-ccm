package ccm.models.system.justin;

import java.util.List;

public class JustinCourtAppearanceSummaryList {
  private String mdoc_justin_no;

  private List<JustinCourtAppearanceSummary> apprsummary;

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }

  public List<JustinCourtAppearanceSummary> getApprsummary() {
    return apprsummary;
  }
  public void setApprsummary(List<JustinCourtAppearanceSummary> apprsummary) {
    this.apprsummary = apprsummary;
  }

}