{{/* Define common labels */}}
{{- define "ccm-app.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/* Default selector labels, used for new installs */}}
{{- define "ccm-app.defaultSelectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Deployment selector labels.
A Deployment's selector is immutable, and Deployments created by the old redeploy
scripts use different selectors (e.g. app.kubernetes.io/name: <app>, or app: jade-ccm).
Reuse the live selector when the Deployment already exists so `helm upgrade` can
patch it in place; fall back to the chart defaults for new installs.
*/}}
{{- define "ccm-app.selectorLabels" -}}
{{- $live := lookup "apps/v1" "Deployment" .Release.Namespace (include "ccm-app.fullname" .) -}}
{{- $liveSelector := dig "spec" "selector" "matchLabels" dict $live -}}
{{- if $liveSelector -}}
{{- toYaml $liveSelector -}}
{{- else -}}
{{- include "ccm-app.defaultSelectorLabels" . -}}
{{- end -}}
{{- end }}

{{/*
Pod template labels: the chart defaults overlaid with the Deployment selector
(the selector must win so it still matches). Every pod also carries the unique
app.kubernetes.io/instance label for Service/PDB selection.
*/}}
{{- define "ccm-app.podLabels" -}}
{{- $labels := include "ccm-app.defaultSelectorLabels" . | fromYaml -}}
{{- $labels = merge (include "ccm-app.selectorLabels" . | fromYaml) $labels -}}
{{- $_ := set $labels "app.kubernetes.io/instance" .Release.Name -}}
{{- toYaml $labels -}}
{{- end }}

{{/*
Service selector: keep the live Service's selector only if it still matches the
pods, otherwise select on the unique instance label. Never reuse a broad Deployment selector such as
app: jade-ccm, which matches every CCM pod in prod.
*/}}
{{- define "ccm-app.serviceSelectorLabels" -}}
{{- $live := lookup "v1" "Service" .Release.Namespace (include "ccm-app.fullname" .) -}}
{{- $liveSelector := dig "spec" "selector" dict $live -}}
{{- $podLabels := include "ccm-app.podLabels" . | fromYaml -}}
{{- $matches := gt (len $liveSelector) 0 -}}
{{- range $k, $v := $liveSelector -}}
{{- if ne (get $podLabels $k) $v -}}{{- $matches = false -}}{{- end -}}
{{- end -}}
{{- if $matches -}}
{{- toYaml $liveSelector -}}
{{- else -}}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
{{- end }}

{{/* PDB selector: keep the live PDB's selector only if it still matches the pods, otherwise the unique instance label */}}
{{- define "ccm-app.pdbSelectorLabels" -}}
{{- $live := lookup "policy/v1" "PodDisruptionBudget" .Release.Namespace (include "ccm-app.fullname" .) -}}
{{- $liveSelector := dig "spec" "selector" "matchLabels" dict $live -}}
{{- $podLabels := include "ccm-app.podLabels" . | fromYaml -}}
{{- $matches := gt (len $liveSelector) 0 -}}
{{- range $k, $v := $liveSelector -}}
{{- if ne (get $podLabels $k) $v -}}{{- $matches = false -}}{{- end -}}
{{- end -}}
{{- if $matches -}}
{{- toYaml $liveSelector -}}
{{- else -}}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
{{- end }}

{{/*
Env vars the live Deployment sets as a plain value. Used to avoid
"valueFrom may not be specified when value is not empty" when the chart
switches a variable (e.g. POD_NAME) from value to valueFrom.
*/}}
{{- define "ccm-app.liveEnvValues" -}}
{{- $live := lookup "apps/v1" "Deployment" .Release.Namespace (include "ccm-app.fullname" .) -}}
{{- $values := dict -}}
{{- $containers := dig "spec" "template" "spec" "containers" list $live -}}
{{- if $containers -}}
{{- range (index $containers 0).env -}}
{{- if hasKey . "value" -}}
{{- $_ := set $values .name .value -}}
{{- end -}}
{{- end -}}
{{- end -}}
{{- toYaml $values -}}
{{- end }}

{{/* Define full name */}}
{{- define "ccm-app.fullname" -}}
{{ .Release.Name }}{{ .Values.deploySuffix | default "" }}
{{- end }}
