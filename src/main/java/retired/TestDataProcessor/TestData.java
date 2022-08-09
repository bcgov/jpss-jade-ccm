import java.util.List;

//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class TestData {
  private int id;
  private String phone;
  private List<String> names;
  private List<TestContactDetails> contacts;

  public int getId() {
    return this.id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getPhone() {
    return this.phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public List<String> getNames() {
    return this.names;
  }

  public void setNames(List<String> names) {
    this.names = names;
  }

  public List<TestContactDetails> getContacts() {
    return this.contacts;
  }

  public void setContacts(List<TestContactDetails> contacts) {
    this.contacts = contacts;
  }
}