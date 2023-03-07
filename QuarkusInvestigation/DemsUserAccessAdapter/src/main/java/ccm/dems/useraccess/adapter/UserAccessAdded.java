package ccm.dems.useraccess.adapter;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import ccm.dems.useraccess.adapter.models.common.data.AuthUser;
import ccm.dems.useraccess.adapter.models.common.data.AuthUserList;
import ccm.dems.useraccess.adapter.models.event.BaseEvent;
import ccm.dems.useraccess.adapter.models.event.CaseUserEvent;
import ccm.dems.useraccess.adapter.models.event.EventKPI;
import ccm.dems.useraccess.adapter.models.system.dems.*;
import ccm.dems.useraccess.adapter.utils.DateTimeUtils;
import org.apache.camel.CamelException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserAccessAdded extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessAdded.class);
    // private static final Logger LOG = Logger.getLogger(UserAccessAdded.class);
    private static final String AdapterName = "CcmDemsUserAccessAdapter";

    @Override
    public void configure() throws Exception {

        attachExceptionHandlers();
        processCaseUserEvents();
        publishEventKPI();
        processCaseUserAccessAdded();
        processCourtCaseAuthListChanged();
        processCourtCaseAuthListUpdated();
        syncCaseUserList();
        getGroupMapByCaseId();
        getCourtCaseDataById();

        getCourtCaseIdByKey();
        prepareDemsCaseGroupMembersSyncHelperList();
        syncCaseGroupMembers();



    }

    private void attachExceptionHandlers() {

        // handle network connectivity errors
        onException(ConnectException.class, SocketTimeoutException.class).backOffMultiplier(2)
                .log(LoggingLevel.ERROR, "onException(ConnectException, SocketTimeoutException) called.")
                .setBody(constant("An unexpected network error occurred")).retryAttemptedLogLevel(LoggingLevel.ERROR)
                .handled(true).end();

        // HttpOperation Failed
        onException(HttpOperationFailedException.class).process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                BaseEvent event = (BaseEvent) exchange.getProperty("kpi_event_object");
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                ccm.dems.useraccess.adapter.models.event.Error error = new ccm.dems.useraccess.adapter.models.event.Error();
                error.setError_dtm(DateTimeUtils.generateCurrentDtm());
                error.setError_code("HttpOperationFailed");
                error.setError_summary("Unable to process event.HttpOperationFailed exception raised");

                log.debug("HttpOperationFailed caught, exception message : " + cause.getMessage() + " stack trace : "
                        + cause.getStackTrace());
                log.error("HttpOperationFailed Exception event info : " + event.getEvent_source());
                // KPI
                EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
                String kafkaTopic = getKafkaTopicByEventType(event.getEvent_type());
                kpi.setEvent_topic_name(kafkaTopic);
                kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
                kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
                kpi.setComponent_route_name((String) exchange.getProperty("kpi_component_route_name"));
                kpi.setError(error);
                exchange.getMessage().setBody(kpi);
            }
        }).marshal().json(JsonLibrary.Jackson, EventKPI.class)
                .log(LoggingLevel.ERROR, "Publishing derived event KPI in Exception handler ...")
                .log(LoggingLevel.DEBUG, "Derived event KPI published.").log("Caught HttpOperationFailed exception")
                .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
                .setProperty("error_event_object", body()).handled(true).to("kafka:{{kafka.topic.kpis.name}}").end();

        // Camel Exception
        onException(CamelException.class).process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                BaseEvent event = (BaseEvent) exchange.getProperty("kpi_event_object");
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                ccm.dems.useraccess.adapter.models.event.Error error = new ccm.dems.useraccess.adapter.models.event.Error();
                error.setError_dtm(DateTimeUtils.generateCurrentDtm());
                error.setError_code("CamelException");
                error.setError_summary("Unable to process event, CamelException raised.");

                log.error("CamelException caught, exception message : " + cause.getMessage() + " stack trace : "
                        + cause.getStackTrace());
                log.error("CamelException Exception event info : " + event.getEvent_source());

                // KPI
                EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
                String kafkaTopic = getKafkaTopicByEventType(event.getEvent_type());

                kpi.setEvent_topic_name(kafkaTopic);
                kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
                kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
                kpi.setComponent_route_name((String) exchange.getProperty("kpi_component_route_name"));
                kpi.setError(error);
                exchange.getMessage().setBody(kpi);
            }
        }).marshal().json(JsonLibrary.Jackson, EventKPI.class)
                .log(LoggingLevel.ERROR, "Publishing derived event KPI in Exception handler ...")
                .log(LoggingLevel.DEBUG, "Derived event KPI published.").log("Caught CamelException exception")
                .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
                .setProperty("error_event_object", body()).to("kafka:{{kafka.topic.kpis.name}}").handled(true).end();

        // General Exception
        onException(Exception.class).process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                BaseEvent event = (BaseEvent) exchange.getProperty("kpi_event_object");
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                ccm.dems.useraccess.adapter.models.event.Error error = new ccm.dems.useraccess.adapter.models.event.Error();
                error.setError_dtm(DateTimeUtils.generateCurrentDtm());
                error.setError_summary("Unable to process event., general Exception raised.");
                error.setError_code("General Exception");
                error.setError_details(event);
                log.error("General Exception class and local msg : " + cause.getClass().getName() + " message : "
                        + cause.getLocalizedMessage());

                log.error("General Exception caught, exception message : " + cause.getMessage() + " stack trace : "
                        + cause.getStackTrace());
                log.error("General Exception event info : " + event.getEvent_source());
                // KPI
                EventKPI kpi = new EventKPI(event, EventKPI.STATUS.EVENT_PROCESSING_FAILED);
                String kafkaTopic = getKafkaTopicByEventType(event.getEvent_type());

                kpi.setEvent_topic_name(kafkaTopic);
                kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
                kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
                kpi.setComponent_route_name((String) exchange.getProperty("kpi_component_route_name"));
                kpi.setError(error);
                exchange.getMessage().setBody(kpi);
            }
        }).marshal().json(JsonLibrary.Jackson, EventKPI.class)
                .log(LoggingLevel.ERROR, "Publishing derived event KPI in Exception handler ...")
                .log(LoggingLevel.DEBUG, "Derived event KPI published.").log("Caught General exception exception")
                .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_FAILED.name()))
                .setProperty("error_event_object", body()).to("kafka:{{kafka.topic.kpis.name}}").handled(true).end();

    }

    private void getCourtCaseDataById() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN: exchangeProperty.id
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "Processing request (id=${exchangeProperty.id})...")
                .removeHeader("CamelHttpUri").removeHeader("CamelHttpBaseUri").removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setHeader("Authorization")
                .simple("Bearer " + "{{dems.token}}").toD("https://{{dems.host}}/cases/${exchangeProperty.id}")
                .log(LoggingLevel.DEBUG, "Retrieved court case data by id.");
    }

    private String getKafkaTopicByEventType(String eventType) {
        String kafkaTopic = "ccm-general-errors";
        if (eventType != null) {
            switch (eventType) {
            case "ChargeAssessmentEvent":
                kafkaTopic = "ccm-chargeassessment-errors";
                break;
            case "CaseUserEvent": {
                kafkaTopic = "ccm-caseuser-errors";
                break;
            }
            case "CourtCaseEvent": {
                kafkaTopic = "ccm-courtcase-errors";
                break;
            }
            }
        }
        return kafkaTopic;
    }

    private void processCaseUserEvents() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PropertiesComponent pc = (PropertiesComponent) this.getCamelContext().getPropertiesComponent();
        pc.setLocation("classpath:application.properties");
        pc.loadProperties();
        // from("kafka:{{kafka.topic.chargeassessments.name}}?groupId=ccm-notification-service")
        from("kafka:{{kafka.topic.caseusers.name}}?groupId=ccm-notification-service").routeId(routeId).log(
                LoggingLevel.INFO,
                "Event from Kafka {{kafka.topic.chargeassessments.name}} topic (offset=${headers[kafka.OFFSET]}): ${body}\n"
                        + "    on the topic ${headers[kafka.TOPIC]}\n"
                        + "    on the partition ${headers[kafka.PARTITION]}\n"
                        + "    with the offset ${headers[kafka.OFFSET]}\n" + "    with the key ${headers[kafka.KEY]}")
                .setHeader("event_key").jsonpath("$.event_key").setHeader("event_status").jsonpath("$.event_status")
                .setHeader("event").simple("${body}").unmarshal().json(JsonLibrary.Jackson, CaseUserEvent.class)
                .setProperty("event_object", body()).setProperty("kpi_event_object", body())
                .setProperty("kpi_event_topic_name", simple("${headers[kafka.TOPIC]}"))
                .setProperty("kpi_event_topic_offset", simple("${headers[kafka.OFFSET]}")).marshal()
                .json(JsonLibrary.Jackson, CaseUserEvent.class).choice()
                .when(header("event_status").isEqualTo(CaseUserEvent.STATUS.ACCESS_ADDED))
                .setProperty("kpi_component_route_name", simple("processCaseUserAccessAdded"))
                .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_STARTED.name()))
                .to("direct:publishEventKPI").setBody(header("event")).to("direct:processCaseUserAccessAdded")
                .setProperty("kpi_status", simple(EventKPI.STATUS.EVENT_PROCESSING_COMPLETED.name()))
                .to("direct:publishEventKPI").endChoice().end();
    }

    private void processCaseUserAccessAdded() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // LOG.debug("### PROCESS CASE USER ACCESS ADDED CALLED");
        // IN
        // property: event_object
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "event_key = ${header[event_key]}").process(new Processor() {

                    @Override
                    public void process(Exchange exchange) {
                        CaseUserEvent event = (CaseUserEvent) exchange.getProperty("event_object");
                        exchange.getMessage().setHeader("event_key", event.getJustin_rcc_id());
                    }
                })
                .log(LoggingLevel.INFO,
                        "Calling route processCourtCaseAuthListChanged( rcc_id = ${header[event_key]} ) ...")
                .to("direct:processCourtCaseAuthListChanged")
                .log(LoggingLevel.DEBUG, "Returned from processCourtCaseAuthListChanged().");
    }

    private void processCourtCaseAuthListChanged() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();
        // LOG.debug("### processCourtCaseAuthListChanged");
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.INFO, "event_key = ${header[event_key]}")
                .setHeader("number", simple("${header[event_key]}")).to("http://ccm-lookup-service/getCourtCaseExists")
                .unmarshal().json().setProperty("caseFound").simple("${body[id]}").setProperty("autoCreateFlag")
                .simple("false").choice().when(simple("${exchangeProperty.caseFound} != ''"))
                // .simple("{{dems.case.auto.creation}}").choice().when(simple("${exchangeProperty.caseFound} != ''"))
                .to("direct:processCourtCaseAuthListUpdated").end();
    }

    private void processCourtCaseAuthListUpdated() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        logger.debug("### processCourtCaseAuthListUpdated");
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "event_key = ${header[event_key]}")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setHeader("number")
                .simple("${header.event_key}").log(LoggingLevel.DEBUG, "Retrieve court case auth list")
                .to("http://ccm-lookup-service/getCourtCaseAuthList")
                .log(LoggingLevel.INFO, "Update court case auth list in DEMS.  Court case auth list = ${body}")
                // JADE-1489 work around #1 -- not sure why body doesn't make it into dems-adapter
                .setHeader("temp-body", simple("${body}")).to("direct:syncCaseUserList");
    }

    // DEMS Adapter code
    private void syncCaseUserList() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.INFO, "Processing request: ${body}").unmarshal()
                .json(JsonLibrary.Jackson, AuthUserList.class).process(new Processor() {
                    public void process(Exchange exchange) {
                        AuthUserList b = exchange.getIn().getBody(AuthUserList.class);
                        DemsAuthUsersList da = new DemsAuthUsersList(b);
                        exchange.getMessage().setBody(da);
                        exchange.setProperty("auth_user_list_object", b);
                    }
                }).marshal().json(JsonLibrary.Jackson, DemsAuthUsersList.class).setProperty("dems_auth_user_list")
                .simple("${body}").log(LoggingLevel.INFO, "DEMS-bound case users sync request data: '${body}'.")
                .setProperty("sync_data", simple("${body}")).log(LoggingLevel.INFO, "calling getCourtCaseById")
                // get case id
                // exchangeProperty.key already set
                .to("direct:getCourtCaseIdByKey").setProperty("dems_case_id", jsonpath("$.id"))
                // sync case users
                .removeHeader("CamelHttpUri").removeHeader("CamelHttpBaseUri").removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
                .setBody(simple("${exchangeProperty.dems_auth_user_list}"))
                .log(LoggingLevel.DEBUG, "Synchronizing case users ...")
                .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/case-users/sync")
                .log(LoggingLevel.DEBUG, "Case users synchronized.")
                // retrieve DEMS case group map
                .to("direct:getGroupMapByCaseId")
                // create DEMS case group members sync helper list
                .to("direct:prepareDemsCaseGroupMembersSyncHelperList")
                // sync case group members
                .to("direct:syncCaseGroupMembers");
    }

    private void getCourtCaseIdByKey() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();
        // String demsUrl = "wsgw.dev.jag.gov.bc.ca/bcpsdems/api/v1";

        // IN: exchangeProeprty.key
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.INFO, "key = ${exchangeProperty.key}...").removeHeader("CamelHttpUri")
                .log(LoggingLevel.INFO, "dems_host = {{dems-host-url}}").removeHeader("CamelHttpBaseUri")
                // .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                // .removeHeader(Exchange.HTTP_URI).removeHeader(Exchange.HTTP_PATH)
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setHeader("Authorization")
                .simple("Bearer {{dems.token}}")
                .toD("https://wsgw.dev.jag.gov.bc.ca/bcpsdems/api/v1/org-units/1/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")

                // .toD("https://" + demsUrl +
                // "${dems.org-unit.id}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
                // .toD("http://httpstat.us:443/500") // --> testing code, remove later
                // .toD("rest:get:org-units/{{dems.org-unit.id}}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false&host={{dems.host}}&bindingMode=json&ssl=true")
                // .toD("netty-http:https://{{dems.host}}/org-units/{{dems.org-unit.id}}/cases/${exchangeProperty.key}/id?throwExceptionOnFailure=false")
                .setProperty("length", jsonpath("$.length()")).choice()
                .when(simple("${header.CamelHttpResponseCode} == 200 && ${exchangeProperty.length} > 0"))
                .setProperty("id", jsonpath("$[0].id")).setBody(simple("{\"id\": \"${exchangeProperty.id}\"}"))
                .endChoice().when(simple("${header.CamelHttpResponseCode} == 200"))
                .log(LoggingLevel.DEBUG, "body = '${body}'.").setProperty("id", simple(""))
                .setBody(simple("{\"id\": \"\"}")).setHeader("CamelHttpResponseCode", simple("200"))
                .log(LoggingLevel.DEBUG, "Case not found.").endChoice().end();
    }

    private void getGroupMapByCaseId() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN: exchangeProperty.dems_case_id
        // OUT: exchangeProperty.dems_case_group_map object
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .removeHeader("CamelHttpUri").removeHeader("CamelHttpBaseUri").removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setHeader("Authorization")
                .simple("Bearer " + "{{dems.token}}")
                .log(LoggingLevel.DEBUG, "Looking up case groups (case id = ${exchangeProperty.dems_case_id}) ...")
                .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups")
                // create initial case group map
                .convertBodyTo(String.class).process(new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        DemsCaseGroupMap caseGroupMap = new DemsCaseGroupMap((String) exchange.getIn().getBody());
                        exchange.setProperty("dems_case_group_map", caseGroupMap);
                    }
                });
    }

    private void prepareDemsCaseGroupMembersSyncHelperList() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN: exchangeProperty.auth_user_list_object
        // IN: exchangeProperty.dems_case_group_map
        // ---
        // OUT: exchangeProperty.dems_case_group_members_sync_helper_list
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .process(new Processor() {
                    public void process(Exchange exchange) {
                        AuthUserList userList = (AuthUserList) exchange.getProperty("auth_user_list_object");
                        DemsCaseGroupMap demsCaseGroupMapForCase = (DemsCaseGroupMap) exchange
                                .getProperty("dems_case_group_map");

                        List<DemsCaseGroupMembersSyncHelper> demsGroupMembersSyncHelperList = new ArrayList<DemsCaseGroupMembersSyncHelper>();

                        // create an empty DEMS case group members map with all case groups.
                        HashMap<String, DemsCaseGroupMembersSyncData> demsGroupMembersMapByName = new HashMap<String, DemsCaseGroupMembersSyncData>();
                        for (DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS demsCaseGroup : DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS
                                .values()) {
                            DemsCaseGroupMembersSyncData emptySyncData = new DemsCaseGroupMembersSyncData();
                            demsGroupMembersMapByName.put(demsCaseGroup.getDems_name(), emptySyncData);
                        }

                        // iterate through auth user list
                        for (AuthUser user : userList.getAuth_user_list()) {
                            DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS demsCaseGroupListMapping = DemsListItemFieldData.CASE_GROUP_FIELD_MAPPINGS
                                    .findCaseGroupByJustinName(user.getJrs_role());

                            String demsCaseGroupName = (demsCaseGroupListMapping == null) ? null
                                    : demsCaseGroupListMapping.getDems_name();
                            Long demsCaseGroupId = (demsCaseGroupListMapping == null) ? null
                                    : demsCaseGroupMapForCase.getIdByName(demsCaseGroupName);

                            if (demsCaseGroupId != null) {
                                DemsCaseGroupMembersSyncData syncData = demsGroupMembersMapByName
                                        .get(demsCaseGroupName);

                                // check if sync data is found
                                if (syncData != null) {
                                    // add user to sync data
                                    syncData.getValues().add(user.getPart_id());
                                    System.out.println(
                                            "DEBUG: User added to sync data for DEMS group '" + demsCaseGroupName
                                                    + "' (id=" + demsCaseGroupId + "), user id = " + user.getPart_id());
                                }
                            } else {
                                System.out.println("ERROR: Cannot add user sync data for DEMS group '"
                                        + demsCaseGroupName + "' (id=" + demsCaseGroupId + "), user id = "
                                        + user.getPart_id() + ", user JRS role = " + user.getJrs_role());
                            }
                        }

                        for (String actualDemsCaseGroupName : demsCaseGroupMapForCase.getMap().keySet()) {
                            Long actualDemsCaseGroupId = demsCaseGroupMapForCase.getIdByName(actualDemsCaseGroupName);
                            DemsCaseGroupMembersSyncData syncData = demsGroupMembersMapByName
                                    .get(actualDemsCaseGroupName);
                            DemsCaseGroupMembersSyncHelper helper = null;

                            if (syncData != null) {
                                // add sync data to helper list
                                helper = new DemsCaseGroupMembersSyncHelper(actualDemsCaseGroupId,
                                        actualDemsCaseGroupName, syncData);
                                System.out.println("DEBUG: found case group: " + actualDemsCaseGroupName);
                            } else {
                                // add empty sync data to helper list
                                DemsCaseGroupMembersSyncData emptySyncData = new DemsCaseGroupMembersSyncData();
                                helper = new DemsCaseGroupMembersSyncHelper(actualDemsCaseGroupId,
                                        actualDemsCaseGroupName, emptySyncData);
                            }

                            demsGroupMembersSyncHelperList.add(helper);
                        }
                        exchange.setProperty("dems_case_group_members_sync_helper_list",
                                demsGroupMembersSyncHelperList);
                    }
                });
    }

    private void syncCaseGroupMembers() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN: exchangeProperty.dems_case_id
        // IN: exchangeProperty.dems_case_group_members_sync_helper_list
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .log(LoggingLevel.DEBUG, "Case group sync processing started.").removeHeader("CamelHttpUri")
                .removeHeader("CamelHttpBaseUri").removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("Authorization", simple("Bearer " + "{{dems.token}}"))
                .setBody(simple("${exchangeProperty.dems_case_group_members_sync_helper_list}")).marshal().json()
                .log(LoggingLevel.DEBUG, "body = '${body}'").split().jsonpathWriteAsString("$")
                .setProperty("dems_case_group_name", jsonpath("$.caseGroupName"))
                .setProperty("dems_case_group_id", jsonpath("$.caseGroupId")).unmarshal()
                .json(JsonLibrary.Jackson, DemsCaseGroupMembersSyncHelper.class).process(new Processor() {
                    public void process(Exchange exchange) {
                        DemsCaseGroupMembersSyncHelper helper = (DemsCaseGroupMembersSyncHelper) exchange.getIn()
                                .getBody();
                        exchange.getMessage().setBody(helper.getSyncData());
                    }
                }).marshal().json(JsonLibrary.Jackson, DemsCaseGroupMembersSyncData.class)
                .log(LoggingLevel.DEBUG,
                        "Syncing case group (name='${exchangeProperty.dems_case_group_name}', id='${exchangeProperty.dems_case_group_id}'). sync data = '${body}' ...")
                .toD("https://{{dems.host}}/cases/${exchangeProperty.dems_case_id}/groups/${exchangeProperty.dems_case_group_id}/sync")
                .log(LoggingLevel.DEBUG,
                        "Case group (name='${exchangeProperty.dems_case_group_name}', id='${exchangeProperty.dems_case_group_id}') members synchronized.")
                .end().log(LoggingLevel.DEBUG, "Case group sync processing completed.");
    }

    private void publishEventKPI() {
        // use method name as route id
        String routeId = new Object() {
        }.getClass().getEnclosingMethod().getName();

        // IN: property = kpi_event_object
        // IN: property = kpi_event_topic_name
        // IN: property = kpi_event_topic_offset
        // IN: property = kpi_status
        // IN: property = kpi_component_route_name
        from("direct:" + routeId).routeId(routeId).streamCaching() // https://camel.apache.org/manual/faq/why-is-my-message-body-empty.html
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        BaseEvent event = (BaseEvent) exchange.getProperty("kpi_event_object");
                        String kpi_status = (String) exchange.getProperty("kpi_status");

                        // KPI
                        EventKPI kpi = new EventKPI(event, kpi_status);
                        kpi.setKpi_dtm(DateTimeUtils.generateCurrentDtm());
                        kpi.setEvent_topic_name((String) exchange.getProperty("kpi_event_topic_name"));
                        kpi.setEvent_topic_offset(exchange.getProperty("kpi_event_topic_offset"));
                        kpi.setIntegration_component_name(this.getClass().getEnclosingClass().getSimpleName());
                        kpi.setComponent_route_name((String) exchange.getProperty("kpi_component_route_name"));
                        kpi.setIntegration_component_name(AdapterName);
                        exchange.getMessage().setBody(kpi);

                    }
                }).marshal().json(JsonLibrary.Jackson, EventKPI.class).log(LoggingLevel.DEBUG, "Event kpi: ${body}")
                .to("kafka:{{kafka.topic.kpis.name}}");
    }

}
