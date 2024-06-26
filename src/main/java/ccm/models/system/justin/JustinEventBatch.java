package ccm.models.system.justin;
import java.util.List;

public class JustinEventBatch {
  // Unmarshalling a JSON Array Using camel-jackson
  // https://www.baeldung.com/java-camel-jackson-json-array

  private List<JustinEvent> events;

  public List<JustinEvent> getEvents() {
    return this.events;
  }

  public void setEvents(List<JustinEvent> events) {
    this.events = events;
  }
}