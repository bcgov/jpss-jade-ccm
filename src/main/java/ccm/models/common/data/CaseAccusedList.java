package ccm.models.common.data;

import java.util.ArrayList;
import java.util.List;

public class CaseAccusedList {
    private List<CaseAccused> caseAccused;

    public CaseAccusedList() {
      caseAccused = new ArrayList<CaseAccused>();
    }

    public List<CaseAccused> getCaseAccused() {
      return caseAccused;
    }

    public void setCaseAccused(List<CaseAccused> caseAccused) {
      this.caseAccused = caseAccused;
    }

    
  }
                                               