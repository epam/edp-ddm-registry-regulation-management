{{/*
Expand the name of the chart.
*/}}
{{- define "registry-regulation-management.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "keycloak.url" -}}
{{- printf "%s%s" "https://" .Values.keycloak.host }}
{{- end -}}

{{- define "keycloak.customUrl" -}}
{{- printf "%s%s" "https://" .Values.keycloak.customHost }}
{{- end -}}

{{- define "keycloak.urlPrefix" -}}
{{- printf "%s%s%s" (include "keycloak.url" .) "/auth/realms/" .Release.Namespace -}}
{{- end -}}

{{- define "keycloak.customUrlPrefix" -}}
{{- printf "%s%s%s" (include "keycloak.customUrl" .) "/auth/realms/" .Release.Namespace -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "registry-regulation-management.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "registry-regulation-management.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "registry-regulation-management.labels" -}}
helm.sh/chart: {{ include "registry-regulation-management.chart" . }}
{{ include "registry-regulation-management.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "registry-regulation-management.selectorLabels" -}}
app.kubernetes.io/name: {{ include "registry-regulation-management.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "registry-regulation-management.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "registry-regulation-management.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "issuer.officer" -}}
{{- if .Values.keycloak.customHost }}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.officer -}}
{{- else }}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.officer -}}
{{- end }}
{{- end -}}

{{- define "issuer.citizen" -}}
{{- if .Values.keycloak.customHost }}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.citizen -}}
{{- else }}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.citizen -}}
{{- end }}
{{- end -}}

{{- define "issuer.admin" -}}
{{- if .Values.keycloak.customHost }}
{{- printf "%s-%s" (include "keycloak.customUrlPrefix" .) .Values.keycloak.realms.admin -}}
{{- else }}
{{- printf "%s-%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.admin -}}
{{- end }}
{{- end -}}

{{- define "jwksUri.officer" -}}
{{- printf "%s-%s%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.officer .Values.keycloak.certificatesEndpoint -}}
{{- end -}}

{{- define "jwksUri.citizen" -}}
{{- printf "%s-%s%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.citizen .Values.keycloak.certificatesEndpoint -}}
{{- end -}}

{{- define "jwksUri.admin" -}}
{{- printf "%s-%s%s" (include "keycloak.urlPrefix" .) .Values.keycloak.realms.admin .Values.keycloak.certificatesEndpoint -}}
{{- end -}}

{{/*
Create officer-portal realm name in Keycloak
*/}}
{{- define "keycloak.officerRealm" -}}
{{- printf "%s-%s" .Values.namespace .Values.keycloak.officerRealmName }}
{{- end -}}