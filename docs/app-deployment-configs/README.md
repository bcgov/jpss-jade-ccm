# ISL-CCM ConfigMap Object Definition (ccm-configs)

| Key | Description | Type | 
| - | - | - |
| dems-case-auto-creation | DEMS case auto creation flag | true / false |
| dems-case-auto-creation-submit-date-cutoff | Earliest RCC Submit date to allow auto creation | YYYY-MM-DD |
| dems-case-template-id | DEMS case template id | numeric |
| dems-host-url | DEMS URI | Host URI (without HTTP prefix) |
| dems-org-unit-id | DEMS case organization unit id | numeric |
| dems-case-hyperlink-prefix | DEMS case hyperlink prefix | string |
| dems-case-hyperlink-suffix | DEMS case hyperlink suffix | string |
| dems-case-hyperlinklist-suffix | DEMS case hyperlink list suffix | string |
| justin-host-url | JUSTIN URI | Host URI (without HTTP prefix) |
| splunk-host-url | Splunk URI | Host URI (without HTTP prefix) |
| pidp-kafka-topic-usercreation-name | PIDP Kafka User Creation Topic Name | string |
| pidp-kafka-topic-processresponse-name | PIDP Kafka User Process Response Topic Name | string |
| pidp-kafka-oauth-token-endpoint-url | PIDP OAuth Token Endpoint URI | Host URI (with HTTP prefix) |
| pidp-kafka-bootstrapservers-url | PIDP Kafka Boostrap Servers URI | Host URI (without HTTP prefix) |
| pidp-kafka-apicurioregistry-url | PIDP Kafka Apicurio Registry URI | Host URI (with HTTP prefix) |
| kafka-broker-url | ISL Events Kafka Broker URI | string |
| pidp-kafka-oauth-client-id | PIDP Oauth Client Id | string |
| pidp-api-oauth-token-endpoint-url | PIDP API Oauth Token URI | Host URI (with HTTP prefix) |
| pidp-api-oauth-client-id | PIDP API OAuth client id | string |

# ISL-CCM Secret Object Definition (ccm-secrets)

| Key | Description | Type | Source |
| - | - | - | - |
| dems-adapter-secret-token | DEMS Adapter security token (deprecated) | masked string | generated |
| dems-security-token | DEMS API security token | masked string | ISB API Gateway |
| justin-security-token | JUSTIN API security token | masked string | ISB API Gateway |
| justin-in-security-token | JUSTIN Inbound API security token | masked string | generated |
| splunk-security-token | Splunk API security token | masked string | ISB Splunk App |
| pidp-kafka-oauth-client-secret | PIDP Oauth client secret | masked string | DIAM OAuth Server (Keycloak) |
| pidp-kafka-client.p12-password | PIDP Kafka client PKCS (Java keystore) file password | masked string | DIAM PIDP Kafka Secret/Key (pidp-kafka-cluster-clients-ca-cert/ca.password) |
| pidp-kafka-server.p12-password | PIDP Kafka server PKCS (Java truststore) file password | masked string | DIAM PIDP Kafka Secret/Key (pidp-kafka-cluster-cluster-ca-cert/ca.password) |
| pidp-kafka-client.p12 | PIDP Kafka client PKCS (Java keystore) file | masked string | DIAM PIDP Kafka Secret/Key (pidp-kafka-cluster-clients-ca-cert/ca.p12) |
| pidp-kafka-server.p12 | PIDP Kafka server PKCS (Java truststore) file | masked string | DIAM PIDP Kafka Secret/Key (pidp-kafka-cluster-cluster-ca-cert/ca.p12) |
| pidp-api-oauth-client-secret | PIDP API OAuth client secret | masked string | DIAM OAuth Server (keycloak)