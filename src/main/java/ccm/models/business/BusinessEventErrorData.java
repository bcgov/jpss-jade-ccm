package ccm.models.business;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BusinessEventErrorData {
    private String error_dtm;
    private String error_code;
    private String error_details;

    public BusinessEventErrorData() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        this.error_dtm = formatter.format(date);
    }

    public BusinessEventErrorData(BusinessEventErrorData another) {
        this.error_dtm = another.error_dtm;
        this.error_code = another.error_code;
        this.error_details = another.error_details;
    }

    public String getError_dtm() {
        return error_dtm;
    }

    public void setError_dtm(String error_dtm) {
        this.error_dtm = error_dtm;
    }

    public String getError_code() {
        return error_code;
    }

    public void setError_code(String error_code) {
        this.error_code = error_code;
    }

    public String getError_details() {
        return error_details;
    }

    public void setError_details(String error_details) {
        this.error_details = error_details;
    }
}
