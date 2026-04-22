# Helm-Based Deployment Guide

## Overview

The `ccm-lookup-service` has been converted to use Helm for deployment, enabling easy multinode configuration across different environments.

## Architecture

```
┌─────────────────────────────────────────┐
│ Kafka Infrastructure (Operator-managed)│
│  - Strimzi Operator                     │
│  - Kafka: 3 replicas (multinode)       │
│  - Zookeeper: 3 replicas               │
│  - Service: events-kafka-bootstrap:9092│
└─────────────────────────────────────────┘
                    ↑
                    │ (connects via bootstrap URL)
                    │
┌─────────────────────────────────────────┐
│ Applications (Helm-deployed)            │
│  - ccm-lookup-service                  │
│    - dev: 3 replicas (multinode)       │
│    - test: 2 replicas (multinode)      │
│    - prod: 1 replica                   │
└─────────────────────────────────────────┘
```

## Multinode Configuration

### Replica Counts by Environment

| Environment | Replicas | Log Level | JSON Logging |
|-------------|----------|-----------|--------------|
| dev         | 3        | DEBUG     | false        |
| test        | 2        | INFO      | false        |
| prod        | 1        | INFO      | true         |

### Files Structure

```
Helm/
├── values-ccm-lookup-service.yml          # Base configuration
├── values-ccm-lookup-service-dev.yml      # Dev overrides (3 replicas)
├── values-ccm-lookup-service-test.yml     # Test overrides (2 replicas)
├── values-ccm-lookup-service-prod.yml     # Prod overrides (1 replica)
└── Templates/
    ├── deployment.yml
    ├── service.yml
    ├── config-map.yml
    └── secrets.yml
```

## Deployment Methods

### 1. GitHub Actions (Recommended)

The `.github/workflows/clab-deploy.yml` pipeline automatically uses Helm for `ccm-lookup-service`:

1. Go to GitHub Actions
2. Select "Deploy to OpenShift" workflow
3. Click "Run workflow"
4. Select:
   - **Cluster**: `cef5dd` or `ac13ae`
   - **Application**: `ccm-lookup-service`
   - **Environment**: `dev`, `test`, or `prod`

The pipeline will:
- Build the application Docker image
- Deploy using Helm with environment-specific replicas
- Apply logging and retry configurations

### 2. Manual Helm Deployment

```bash
# Login to OpenShift
oc login <server-url> --token=<token>

# Set environment variables
export APP=ccm-lookup-service
export ENV=dev  # or test, prod
export NAMESPACE=ac13ae-dev  # adjust based on cluster and env

# Deploy using Helm
cd Helm
helm upgrade --install $APP . \
  -f values-${APP}.yml \
  -f values-${APP}-${ENV}.yml \
  --set namespace=$NAMESPACE \
  --set image.namespace=$NAMESPACE \
  --namespace $NAMESPACE \
  --create-namespace \
  --wait \
  --timeout 5m
```

### 3. Local Script Deployment

The `redeploy-ccm-lookup-service` script supports both legacy and Helm modes:

```bash
cd scripts/jade-ccm

# Legacy deployment (uses OpenShift templates)
./redeploy-ccm-lookup-service dev

# Helm deployment (set HELM_DEPLOY=true to build only)
export HELM_DEPLOY=true
./redeploy-ccm-lookup-service dev
# Then run helm upgrade manually
```

## Verifying Deployment

### Check Pod Count

```bash
# Should show 3 pods for dev, 2 for test, 1 for prod
oc get pods -n $NAMESPACE | grep ccm-lookup-service
```

### Check Kafka Connectivity

```bash
# Check logs for Kafka connection
oc logs -n $NAMESPACE -l app.kubernetes.io/name=ccm-lookup-service --tail=50
```

### Verify Load Distribution

```bash
# Check which node each pod is on
oc get pods -n $NAMESPACE -l app.kubernetes.io/name=ccm-lookup-service -o wide
```

## Scaling On-Demand

You can override replicas at deployment time:

```bash
# Scale to 5 replicas in dev (override the default 3)
helm upgrade --install ccm-lookup-service . \
  -f values-ccm-lookup-service.yml \
  -f values-ccm-lookup-service-dev.yml \
  --set replicas=5 \
  --namespace ac13ae-dev
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
oc describe pod -n $NAMESPACE -l app.kubernetes.io/name=ccm-lookup-service

# Check events
oc get events -n $NAMESPACE --sort-by='.lastTimestamp'
```

### Kafka Connection Issues

```bash
# Verify Kafka service is available
oc get svc -n $NAMESPACE | grep kafka

# Test Kafka connectivity from pod
oc exec -n $NAMESPACE <pod-name> -- curl -v telnet://events-kafka-bootstrap:9092
```

### Helm Release Issues

```bash
# List Helm releases
helm list -n $NAMESPACE

# Get release history
helm history ccm-lookup-service -n $NAMESPACE

# Rollback if needed
helm rollback ccm-lookup-service <revision> -n $NAMESPACE
```

## Migration Plan for Other Services

To convert other services to Helm:

1. Create environment-specific values files:
   - `values-<app>-dev.yml`
   - `values-<app>-test.yml`
   - `values-<app>-prod.yml`

2. Update `.github/workflows/clab-deploy.yml`:
   - Add service to Helm deployment condition

3. Update deployment script:
   - Add HELM_DEPLOY check to skip step 4

4. Test deployment in dev environment first

## Benefits of Helm Deployment

- ✅ **Easy multinode configuration** - Just change `replicas` value
- ✅ **Environment-specific settings** - Different configs for dev/test/prod
- ✅ **Rollback capability** - Easy to revert to previous versions
- ✅ **Declarative configuration** - All settings in version-controlled files
- ✅ **Consistent deployments** - Same process across all environments
- ✅ **Override flexibility** - Can override any value at deployment time
