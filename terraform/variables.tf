variable "aws_region" {
  description = "The AWS region to deploy resources in."
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "The EC2 instance type."
  type        = string
  default     = "t3.large"
}

variable "ami_id" {
  description = "The AMI ID for the EC2 instance. Default is latest Amazon Linux 2."
  type        = string
  default     = ""
}

variable "key_name" {
  description = "The name of the EC2 key pair to use for SSH access. Ensure this key pair exists in your AWS account and region."
  type        = string
}

variable "project_name" {
  description = "A name prefix for resources."
  type        = string
  default     = "team-evil-jenkins"
}

variable "vpc_id" {
  description = "The ID of the VPC to deploy the EC2 instance into. If empty, the default VPC will be used."
  type        = string
  default     = ""
}

variable "subnet_id" {
  description = "The ID of the subnet to deploy the EC2 instance into. If empty, a default subnet in the default VPC will be chosen."
  type        = string
  default     = ""
}

variable "ssh_private_key_path" {
  description = "Path to the SSH private key file for Ansible connection (e.g., ~/.ssh/your-key.pem)."
  type        = string
}

variable "ghcr_username" {
  description = "GitHub username for GHCR login."
  type        = string
  sensitive   = true
}

variable "ghcr_pat" {
  description = "GitHub Personal Access Token (PAT) with read:packages scope for GHCR login."
  type        = string
  sensitive   = true
}

variable "admin_cidr" {
  description = "CIDR allowed to reach SSH and admin/monitoring ports (22, 5050, 3001, 9090). Set to your public IP, e.g. \"203.0.113.10/32\". Defaults to loopback so nothing is exposed accidentally."
  type        = string
  default     = "127.0.0.1/32"
}