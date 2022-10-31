package ccm.models.common;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinCourtAppearanceSummaryList;
import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class CommonCaseAppearanceSummaryList {
  private String mdoc_justin_no;

  private List<CommonCaseAppearanceSummary> apprsummary;

  public CommonCaseAppearanceSummaryList() {
  }

  public CommonCaseAppearanceSummaryList(JustinCourtAppearanceSummaryList jasl) {
    setMdoc_justin_no(jasl.getMdoc_justin_no());

    List<CommonCaseAppearanceSummary> appearanceList = new ArrayList<CommonCaseAppearanceSummary>();

    if(jasl.getApprsummary() != null) {
      for (JustinCourtAppearanceSummary jas : jasl.getApprsummary()) {
        CommonCaseAppearanceSummary bcas = new CommonCaseAppearanceSummary(jas);
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

  public List<CommonCaseAppearanceSummary> getApprsummary() {
    return apprsummary;
  }
  public void setApprsummary(List<CommonCaseAppearanceSummary> apprsummary) {
    this.apprsummary = apprsummary;
  }

}