package ccm.models.business;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinCourtAppearanceSummaryList;
import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class BusinessCourtAppearanceSummaryList {
  private String mdoc_justin_no;

  private List<BusinessCourtAppearanceSummary> apprsummary;

  public BusinessCourtAppearanceSummaryList() {
  }

  public BusinessCourtAppearanceSummaryList(JustinCourtAppearanceSummaryList jasl) {
    setMdoc_justin_no(jasl.getMdoc_justin_no());

    List<BusinessCourtAppearanceSummary> appearanceList = new ArrayList<BusinessCourtAppearanceSummary>();

    if(jasl.getApprsummary() != null) {
      for (JustinCourtAppearanceSummary jas : jasl.getApprsummary()) {
        BusinessCourtAppearanceSummary bcas = new BusinessCourtAppearanceSummary(jas);
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

  public List<BusinessCourtAppearanceSummary> getApprsummary() {
    return apprsummary;
  }
  public void setApprsummary(List<BusinessCourtAppearanceSummary> apprsummary) {
    this.apprsummary = apprsummary;
  }

}