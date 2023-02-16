package ccm.dems.useraccess;

import org.eclipse.microprofile.config.ConfigProvider;

public class TestConfig {

    public TestConfig() {
        String configProvValue = ConfigProvider.getConfig().getValue("dems.url", String.class);
        url = configProvValue;
        token = ConfigProvider.getConfig().getValue("dems.token", String.class);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    String url;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCasetemplateId() {
        return casetemplateId;
    }

    public void setCasetemplateId(String casetemplateId) {
        this.casetemplateId = casetemplateId;
    }

    String token;
    String casetemplateId;

    public void TestTheConfig() {

        String configProvValue = ConfigProvider.getConfig().getValue("dems.url", String.class);
        url = configProvValue;

        System.out.println("Config value " + url);
    }
}
