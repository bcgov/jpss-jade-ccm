package ccm.models.system.justin;

public class JustinAgencyFileCaseDetail {
    private JustinAgencyFileStatus justinRcc;
    private JustinDemsCaseStatus demsCase;

    public JustinAgencyFileCaseDetail() {
    }

    public JustinAgencyFileStatus getJustinRcc() {
        return justinRcc;
    }

    public void setJustinRcc(JustinAgencyFileStatus justinRcc) {
        this.justinRcc = justinRcc;
    }

    public JustinDemsCaseStatus getDemsCase() {
        return demsCase;
    }

    public void setDemsCase(JustinDemsCaseStatus demsCase) {
        this.demsCase = demsCase;
    }

}
