# Shared ConfigMap Management

## Overview

The `ccm-quarkus-configs` ConfigMap contains shared configuration used by all CCM applications in a namespace. This includes Kafka, DEMS, JUSTIN, PIDP, and other common settings.

## Files

- **shared-configmap.yml**: Contains the complete shared ConfigMap definition

## Important Configuration

### Kafka Streams Bootstrap Servers

**Required for:** `ccm-accessdedup-processor`

```yaml
quarkus.kafka-streams.bootstrap-servers: events-kafka-bootstrap:9093
```

This configuration was added to fix the Kafka Streams connectivity issue. The application uses port 9093 for Kafka Streams, while regular Kafka consumers use port 9092.

## Deployment

### Initial Setup

To create the shared ConfigMap in a namespace:

```bash
oc apply -f shared-configmap.yml -n cef5dd-dev
oc apply -f shared-configmap.yml -n cef5dd-test
```

### Updating Configuration

When you need to update the shared configuration:

1. Edit `shared-configmap.yml`
2. Apply the changes:
   ```bash
   oc apply -f shared-configmap.yml -n cef5dd-dev
   oc apply -f shared-configmap.yml -n cef5dd-test
   ```
3. Restart affected deployments to pick up the changes:
   ```bash
   oc rollout restart deployment/<deployment-name> -n <namespace>
   ```

## Environment-Specific Overrides

If you need different values for DEV vs TEST:

1. Create environment-specific files:
   - `shared-configmap-dev.yml`
   - `shared-configmap-test.yml`

2. Apply the appropriate file to each namespace

## Notes

- The shared ConfigMap is **not managed by individual Helm charts**
- Changes to this ConfigMap require pod restarts to take effect
- Binary data (like certificates) should be stored in secrets, not this ConfigMap
- The `ccm-quarkus-secrets` Secret contains sensitive configuration (managed separately)

## Version History

- **2026-05-21**: Added `quarkus.kafka-streams.bootstrap-servers` configuration for Kafka Streams support
