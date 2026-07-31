# Shared ConfigMap Management

## Overview

The `ccm-quarkus-configs` ConfigMap contains shared configuration used by all CCM
applications in a namespace (Kafka, DEMS, JUSTIN, PIDP, Splunk, and other common
settings). It is now rendered by the Helm chart per environment, **not** applied
from a single hand-edited manifest.

> **History:** This ConfigMap used to be applied from a single `shared-configmap.yml`
> that only ever held **dev** URLs. Every deploy re-applied it to whatever namespace
> was targeted, so deploys to test/prod overwrote their correct URLs with dev ones.
> That file has been removed. Environment-specific values now live in the per-env
> values files below and can no longer leak across environments.

## Files

- **templates/config-map.yml** — renders the `ccm-quarkus-configs` ConfigMap.
- **values.yaml** — `quarkusConfig.shared`: keys identical across environments.
- **values-dev.yaml** / **values-test.yaml** — env-specific overrides layered over
  `shared` (only the keys that differ, plus any env-only keys).
- **values-prod.yaml** — self-contained (`useShared: false`): renders **only** its own
  keys so prod never inherits a shared default it does not actually have.

## How rendering works

`templates/config-map.yml` merges `quarkusConfig.env` over `quarkusConfig.shared`.
Setting `quarkusConfig.useShared: false` in a per-env file (as prod does) renders only
that environment's `env` block, with no shared merge.

## Deployment

The ConfigMap is applied by the **"Apply shared ConfigMap"** step in
`.github/workflows/deploy.yml` (a manual `workflow_dispatch` pipeline). That step
derives `ENV` from the target namespace and renders the ConfigMap from the matching
`values-<env>.yaml`:

```bash
helm template ccm-app ./Helm \
  -f Helm/values-<env>.yaml \
  --set quarkusConfig.enabled=true \
  -s templates/config-map.yml \
  | oc apply -n <namespace> -f -
```

If no `values-<env>.yaml` exists for the target environment, the step fails loud
rather than applying a generic (potentially wrong-environment) ConfigMap.

To render/apply manually (e.g. for test):

```bash
helm template ccm-app ./Helm -f Helm/values-test.yaml \
  --set quarkusConfig.enabled=true -s templates/config-map.yml \
  | oc apply -n cef5dd-test -f -
```

After a change, restart affected deployments to pick it up:

```bash
oc rollout restart deployment/<deployment-name> -n <namespace>
```

## Adding or changing values

- **A value that is the same everywhere** → edit `quarkusConfig.shared` in `values.yaml`.
- **A value that differs per environment** → edit that environment's `values-<env>.yaml`.
- **A new environment** → create `values-<env>.yaml`. Prefer the self-contained
  (`useShared: false`) form for anything as sensitive as prod.

Validate before deploying by diffing the render against what is live:

```bash
helm template ccm-app ./Helm -f Helm/values-prod.yaml \
  --set quarkusConfig.enabled=true -s templates/config-map.yml
oc get configmap ccm-quarkus-configs -n cef5dd-prod -o yaml
```

## Backups

Point-in-time snapshots of the live ConfigMaps live in `Helm/backup/`
(`*-backup.yaml` = raw dump, `*-restore.yaml` = clean re-appliable manifest) for
dev, test, and prod. Restore with `oc apply -f Helm/backup/<file> -n <namespace>`.

## Notes

- Per-app Helm releases do **not** render this ConfigMap (`quarkusConfig.enabled` is
  `false` by default) so the 10 app releases don't fight over ownership of it. The
  dedicated deploy step enables it explicitly with `--set quarkusConfig.enabled=true`.
- Changes require pod restarts to take effect.
- Binary data (certificates) belongs in secrets, not this ConfigMap.
- The `ccm-quarkus-secrets` Secret holds sensitive configuration (managed separately).

## Version History

- **2026-07-31**: Moved the shared ConfigMap into the Helm chart as per-environment
  values files (`values-dev/test/prod.yaml`); removed the single dev-valued
  `shared-configmap.yml` that was overwriting test/prod with dev URLs.
- **2026-05-21**: Added `quarkus.kafka-streams.bootstrap-servers` configuration for
  Kafka Streams support (dev).
