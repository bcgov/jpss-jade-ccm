package ccm.models.system.splunk;

import java.time.Instant;

import ccm.models.common.event.EventKPI;
import ccm.utils.DateTimeUtils;

public class SplunkEventLog {
    Object event;
    String sourcetype;
    String source;
    String host;
    Long time;

    public SplunkEventLog() {
        this.event = "";
        this.sourcetype = "";
        this.source = "";
        this.host = "";
        this.time = Instant.now().toEpochMilli();
    }

    public SplunkEventLog(String host, EventKPI kpiEvent) {
        //this.event = "Event " + kpiEvent.getEvent_status() + ": " + kpiEvent.getEvent_key();
        this.event = kpiEvent;
        this.sourcetype = kpiEvent.getIntegration_component_name();
        this.source = kpiEvent.getComponent_route_name();
        this.host = host;
        this.time = DateTimeUtils.convertToEpochMilliFromBCDateTimeString(kpiEvent.getKpi_dtm());
    }

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
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

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return sourcetype + ": " + event;
    }
}
