---
title: HelmYS - Helm Templating with YS
hide:
- navigation
- toc
---


[HelmYS](https://github.com/kubeys/helmys) is a
[Kubernetes Helm](https://helm.sh/)
["post renderer"](https://helm.sh/docs/topics/charts/#post-rendering) that lets
you write your Helm chart templates with YS.

YS can be used in combination with Helm's standard
[Go template](https://pkg.go.dev/text/template) syntax or it can replace it
entirely.

When YS is used exclusively, the Helm chart templates are not only
simpler and more concise, but they are also valid YAML files (just like the
`Chart.yaml` and `values.yaml` files).
That means they can be processed with any YAML tools, such as being validated
with a YAML linter like [yamllint](https://www.yamllint.com/).


## Template File Comparisons

This is a side by side comparison of the Helm chart template YAML files created
by `helm create <chart-name>` and then converted to use YS.
The left side is the YS version and the right side is the original Go
template version.

<table>

<tr><td>
```yaml title="ys-chart/templates/helpers.yaml"
!YS-v0
defn trunc(s): take(63 s).str(*).replace(/-$/)

# Expand the name of the chart:
chart-name =:
  trunc: Values.nameOverride ||| Chart.name

# Create a default fully qualified app name.
chart-fullname =:
  if Values.fullnameOverride.?:
    trunc: Values.fullnameOverride
    else:
      name =: Values.nameOverride ||| Chart.name
      if name.has?(Release.Name):
        trunc: Release.Name
        format "%s-%s": Release.Name name

# Selector labels:
selectorLabels =::
  app.kubernetes.io/name:: Chart.name
  app.kubernetes.io/instance:: Release.Name

# Chart labels:
chart-labels =::
  helm.sh/chart:: "$(Chart.name)-$(Chart.version)"
  :: selectorLabels
  app.kubernetes.io/version:: Chart.appVersion
  app.kubernetes.io/managed-by:: Release.Service

# Create the name of the service account to use:
serviceAccountName =:
  Values.serviceAccount.name |||:
    if Values.serviceAccount.create:
      chart-fullname
      'default'
```
</td><td>
```txt title="go-chart/templates/_helpers.tpl"
{{/*
Expand the name of the chart.
*/}}
{{- define "go-chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to
this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "go-chart.fullname" -}}
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
{{- define "go-chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 |
    trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "go-chart.labels" -}}
helm.sh/chart: {{ include "go-chart.chart" . }}
{{ include "go-chart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "go-chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "go-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "go-chart.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "go-chart.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
```
</td></tr>

<tr><td>
```yaml title="ys-chart/templates/deployment.yaml"
!YS-v0:
apiVersion: apps/v1
kind: Deployment
metadata:
  name:: chart-fullname
  labels:: chart-labels
spec:
  :when-not Values.autoscaling.enabled.?::
   replicas:: Values.replicaCount
  selector:
    matchLabels:: selectorLabels
  template:
    metadata:
      :when+ Values.podAnnotations.?::
       annotations:: _
      labels:: chart-labels
    spec:
      :when+ Values.imagePullSecrets.?::
       imagePullSecrets:: _
      serviceAccountName:: serviceAccountName
      securityContext:: Values.podSecurityContext
      containers:
      - name:: Chart.name
        securityContext:: Values.securityContext
        image::
          "$(Values.image.repository):\
           $(Values.image.tag ||| Chart.appVersion)"
        imagePullPolicy:: Values.image.pullPolicy
        ports:
        - name: http
          containerPort:: Values.service.port
          protocol: TCP
        livenessProbe:: Values.livenessProbe
        readinessProbe:: Values.readinessProbe
        resources:: Values.resources
        :when+ Values.volumeMounts.?::
         volumeMounts:: _
      :when+ Values.volumes.?::      {volumes:: _}
      :when+ Values.nodeSelector.?:: {nodeSelector:: _}
      :when+ Values.affinity.?::     {affinity:: _}
      :when+ Values.tolerations.?::  {tolerations:: _}
```
</td><td>
```yaml title="go-chart/templates/deployment.yaml"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "go-chart.fullname" . }}
  labels:
    {{- include "go-chart.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "go-chart.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      {{- with .Values.podAnnotations }}
      annotations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      labels:
        {{- include "go-chart.labels" . | nindent 8 }}
        {{- with .Values.podLabels }}
        {{- toYaml . | nindent 8 }}
        {{- end }}
    spec:
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      serviceAccountName: {{ include "go-chart.serviceAccountName" . }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
        - name: {{ .Chart.Name }}
          securityContext:
            {{- toYaml .Values.securityContext | nindent 12 }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag |
                     default .Chart.AppVersion }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - name: http
              containerPort: {{ .Values.service.port }}
              protocol: TCP
          livenessProbe:
            {{- toYaml .Values.livenessProbe | nindent 12 }}
          readinessProbe:
            {{- toYaml .Values.readinessProbe | nindent 12 }}
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          {{- with .Values.volumeMounts }}
          volumeMounts:
            {{- toYaml . | nindent 12 }}
          {{- end }}
      {{- with .Values.volumes }}
      volumes:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
```
</td></tr>

<tr><td>
```yaml title="ys-chart/templates/serviceaccount.yaml"
!YS-v0:
:when Values.serviceAccount.create:
  apiVersion: v1
  kind: ServiceAccount
  metadata:
    name:: serviceAccountName
    labels:: chart-labels
    :when+ Values.serviceAccount.annotations.?::
     annotations:: _
  automountServiceAccountToken::
    Values.serviceAccount.automount
```
</td><td>
```yaml title="go-chart/templates/serviceaccount.yaml"
{{- if .Values.serviceAccount.create -}}
apiVersion: v1
kind: ServiceAccount
metadata:
  name: {{ include "go-chart.serviceAccountName" . }}
  labels:
    {{- include "go-chart.labels" . | nindent 4 }}
  {{- with .Values.serviceAccount.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
automountServiceAccountToken: {{ .Values.serviceAccount.automount }}
{{- end }}
```
</td></tr>

<tr><td>
```yaml title="ys-chart/templates/service.yaml"
!YS-v0:
apiVersion: v1
kind: Service
metadata:
  name:: chart-fullname
  labels:: chart-labels
spec:
  type:: Values.service.type
  ports:
  - port:: Values.service.port
    targetPort: http
    protocol: TCP
    name: http
  selector:: selectorLabels
```
</td><td>
```yaml title="go-chart/templates/service.yaml"
apiVersion: v1
kind: Service
metadata:
  name: {{ include "go-chart.fullname" . }}
  labels:
    {{- include "go-chart.labels" . | nindent 4 }}
spec:
  type: {{ .Values.service.type }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: http
      protocol: TCP
      name: http
  selector:
    {{- include "go-chart.selectorLabels" . | nindent 4 }}
```
</td></tr>

</table>
