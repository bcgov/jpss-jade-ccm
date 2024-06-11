package ccm.models.system.dems;

public class DemsOrganisationData {

    private String organisationId;

    public DemsOrganisationData() {
    }

    public DemsOrganisationData(String organisationId) {
        setOrganisationId(organisationId);
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

}
