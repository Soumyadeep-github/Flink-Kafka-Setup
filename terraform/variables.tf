variable "namespace" {
  description = "Kubernetes namespace to deploy the streaming platform"
  type        = string
  default     = "kafka-k8s"
}

variable "release_name" {
  description = "Name of the Helm release"
  type        = string
  default     = "streaming-platform"
}

variable "chart_path" {
  description = "Path to the Helm chart"
  type        = string
  default     = "../helm-chart"
}

variable "values_file" {
  description = "Path to the values file"
  type        = string
  default     = "../helm-chart/values-prod.yaml"
}

variable "timeout" {
  description = "Timeout for Helm operations in seconds"
  type        = number
  default     = 600
} 