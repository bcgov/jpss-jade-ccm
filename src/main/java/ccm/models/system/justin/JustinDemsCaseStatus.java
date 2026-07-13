package ccm.models.system.justin;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) 
public class JustinDemsCaseStatus {
    private String id;
    private String message;
    private String status;

    public JustinDemsCaseStatus(String id, String message, String status) {
        this.id = id;
        this.message = message;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
