{{/* Define common labels */}}
{{- define "ccm-app.labels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/* Define selector labels */}}
{{- define "ccm-app.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/* Define full name */}}
{{- define "ccm-app.fullname" -}}
{{ .Release.Name }}-{{ .Chart.Name }}
{{- end }}
