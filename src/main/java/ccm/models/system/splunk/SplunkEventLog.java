package ccm.models.system.splunk;

import ccm.models.common.CommonKPIEvent;

public class SplunkEventLog {
    String event;
    String sourcetype;
    String source;

    public SplunkEventLog() {
        event = "";
        sourcetype = "";
    }

    public SplunkEventLog(CommonKPIEvent kpiEvent) {
        event = "Event " + kpiEvent.getEvent_object_id() + " " + kpiEvent.getEvent_status();
        sourcetype = kpiEvent.getApplication_component_name();
        source = kpiEvent.getComponent_route_id();
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSourcetype() {
        return sourcetype;
    }

    public void setSourcetype(String sourcetype) {
        this.sourcetype = sourcetype;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return sourcetype + ": " + event;
    }
}
