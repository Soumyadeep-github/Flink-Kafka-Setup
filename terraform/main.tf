terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.24"
    }
  }
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

resource "helm_release" "streaming_platform" {
  name       = var.release_name
  repository = null  # Local chart
  chart      = var.chart_path
  namespace  = var.namespace
  create_namespace = true

  values = [
    file(var.values_file)
  ]

  # Set timeout
  timeout = var.timeout

  # Wait for all resources to be ready
  wait = true

  # Atomic installation - if it fails, rollback
  atomic = true

  # Clean up on failure
  cleanup_on_fail = true
} 