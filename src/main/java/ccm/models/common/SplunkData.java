package ccm.models.common;

import ccm.models.common.event.SplunkEvent;

public class SplunkData {
    public static final String DEFAULT_SOURCE_TYPE = "manual";
    public static final String MESSAGE_ID = "Message Id : ";
    public static final String SPACE = " ";

    private String messageId;
    private String event;
    private String sourcetype;

    public SplunkData() {
    }

    public SplunkData(SplunkEvent se) {

        StringBuilder eventMessage = new StringBuilder();
        setMessageId(se.getEvent_object_id());
        if(getMessageId() != null) {
            eventMessage.append(MESSAGE_ID);
            eventMessage.append(getMessageId());
            eventMessage.append(SPACE);
        }
        eventMessage.append(se.getEvent_message());
        setEvent(eventMessage.toString());

        if(se.getSource() != null) {
            setSourcetype(se.getSource());
        } else {
            setSourcetype(DEFAULT_SOURCE_TYPE);
        }

    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

}
