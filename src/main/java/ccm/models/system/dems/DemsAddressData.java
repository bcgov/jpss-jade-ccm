package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsAddressData {

    private String email;

    public DemsAddressData() {
    }

    public DemsAddressData(String email) {
        setEmail(email);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
