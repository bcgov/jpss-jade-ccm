package ccm.dems.useraccess.adapter.models.event.versioning;

public enum Version {
    V1_0("0.24.1");

    private String value;

    private Version(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
