package ccm.models.system.splunk;

import ccm.models.common.event.EventKPI;

public class SplunkEventLog {
    Object event;
    String sourcetype;
    String source;
    String host;

    public SplunkEventLog() {
        this.event = "";
        this.sourcetype = "";
        this.source = "";
        this.host = "";
    }

    public SplunkEventLog(String host, EventKPI kpiEvent) {
        //this.event = "Event " + kpiEvent.getEvent_status() + ": " + kpiEvent.getEvent_key();
        this.event = kpiEvent;
        this.sourcetype = kpiEvent.getIntegration_component_name();
        this.source = kpiEvent.getComponent_route_name();
        this.host = host;
    }

    public Object getEvent() {
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return sourcetype + ": " + event;
    }
}
