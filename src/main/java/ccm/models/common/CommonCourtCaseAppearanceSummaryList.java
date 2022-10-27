package ccm.models.common;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinCourtAppearanceSummaryList;
import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class CommonCourtCaseAppearanceSummaryList {
  private String mdoc_justin_no;

  private List<CommonCourtCaseAppearanceSummary> apprsummary;

  public CommonCourtCaseAppearanceSummaryList() {
  }

  public CommonCourtCaseAppearanceSummaryList(JustinCourtAppearanceSummaryList jasl) {
    setMdoc_justin_no(jasl.getMdoc_justin_no());

    List<CommonCourtCaseAppearanceSummary> appearanceList = new ArrayList<CommonCourtCaseAppearanceSummary>();

    if(jasl.getApprsummary() != null) {
      for (JustinCourtAppearanceSummary jas : jasl.getApprsummary()) {
        CommonCourtCaseAppearanceSummary bcas = new CommonCourtCaseAppearanceSummary(jas);
        appearanceList.add(bcas);
      }
    }

    setApprsummary(appearanceList);
  }

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }

  public List<CommonCourtCaseAppearanceSummary> getApprsummary() {
    return apprsummary;
  }
  public void setApprsummary(List<CommonCourtCaseAppearanceSummary> apprsummary) {
    this.apprsummary = apprsummary;
  }

}