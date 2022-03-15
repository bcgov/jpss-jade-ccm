# Create the 'events' cluster in JADE Dev Silver Cluster

    1. Log in to the [Silver cluster console](https://console.apps.silver.devops.gov.bc.ca/k8s/cluster/projects) (Red Hat Integration POC Lab Environment).
    2. Access the [Red Hat Integration - AMQ Streams](https://console.apps.silver.devops.gov.bc.ca/k8s/ns/cef5dd-dev/operators.coreos.com~v1alpha1~ClusterServiceVersion/amqstreams.v2.0.1-1 ) operator.
    3. Create a new Kafka cluster by selecting the Kafka tab in the operator menu, and click on the "Create Kafka" button to start the creation process.
    4. Select Configure via "YAML view" and paste the configuration file "create-cmn-kafka-cluster-in-dev.yaml" into the window and save the changes.
    5. Click the "Create" button to create the cluster.
    6. Check back on the Kafka tab in the AMQ Streams operator to check on the creation status.  If there are no errors, the status should change from "Condition:NotReady" to "Condition:Ready"
    7. Clicking into the Kafka instance (cmn-kakfa) and select the "Resources" tab will show you the OpenShift artifacts created by the operator.

## Notes: 
    1. PVCs do not show up in the Resources view under the Kafka cluster resources view.
    2. Only one cluster can be created per namespace.
    3. Kafka cluster deletion (through the Kafka tab in the AMQ Streams operator), will delete the kafka PVCs, but not the associated zookeeper PVCs; they will need to be deleted manually.