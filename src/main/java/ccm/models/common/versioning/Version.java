package ccm.models.common.versioning;

public enum Version {
    V1_0("0.20.2");

    private String value;

    private Version(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}