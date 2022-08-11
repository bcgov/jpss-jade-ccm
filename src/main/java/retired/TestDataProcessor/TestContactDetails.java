import java.util.List;

//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//@JsonIgnoreProperties(ignoreUnknown = true)
public class TestContactDetails {
  private String name;
  private String friend_since;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setFriend_since(String friend_since) {
    this.friend_since = friend_since;
  }

  public String getFriend_since() {
    return this.friend_since;
  }

  @Override
  public String toString() {
    return "{\n  \"name\" : \"" + this.name + "\",\n  \"friend_since\" : \"" + this.friend_since + "\"\n}";
  }
}