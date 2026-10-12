variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "cluster_name" {
  type    = string
  default = "product-catalog-cluster"
}

variable "db_password" {
  type      = string
  sensitive = true
  default   = "SecureProdPassword123!" # Override via tfvars or environment variables in production
}