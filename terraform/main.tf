# terraform/main.tf

# --- AWS Provider Configuration ---
provider "aws" {
  region = var.aws_region
}

# --- Data Sources to get AWS information ---
data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# --- Networking Configuration ---
resource "aws_security_group" "web_sg" {
  # Using name_prefix instead of a fixed name to prevent deletion hangs.
  name_prefix = "team-evil-jenkins-web-sg-"
  description = "Allow SSH and web traffic for Docker Compose deployment"
  vpc_id      = var.vpc_id == "" ? data.aws_vpc.default.id : var.vpc_id

  lifecycle {
    create_before_destroy = true
  }

  # Port for SSH access (for Ansible) - restricted to the operator
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  # Port for the NGINX reverse proxy (main application entrypoint)
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Port for pgAdmin (optional admin tool)
  ingress {
    description = "pgAdmin"
    from_port   = 5050
    to_port     = 5050
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  # Port for Grafana (optional monitoring tool)
  ingress {
    description = "Grafana"
    from_port   = 3001
    to_port     = 3001
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  # Port for Prometheus (optional monitoring tool)
  ingress {
    description = "Prometheus"
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }


  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-web-sg"
  }
}

# --- EC2 Instance Configuration ---
resource "aws_instance" "web_server" {
  ami                         = var.ami_id == "" ? data.aws_ami.amazon_linux_2.id : var.ami_id
  instance_type               = var.instance_type
  key_name                    = var.key_name
  subnet_id                   = var.subnet_id == "" ? sort(data.aws_subnets.default.ids)[0] : var.subnet_id
  vpc_security_group_ids      = [aws_security_group.web_sg.id]
  associate_public_ip_address = true

  tags = {
    Name = "${var.project_name}-ec2-instance"
  }

  # This script prepares the server for Ansible
  user_data = <<-EOF
              #!/bin/bash
              sudo systemctl stop httpd
              sudo systemctl disable httpd

              sudo yum update -y
              # Use amazon-linux-extras for a more reliable python install on AL2
              sudo amazon-linux-extras install -y python3.8
              sudo pip3.8 install "urllib3<2.0" requests docker
              
              sudo amazon-linux-extras install docker -y
              sudo service docker start
              sudo usermod -a -G docker ec2-user
              sudo chkconfig docker on

              # Create a flag file to signal completion
              sudo touch /var/lib/cloud/instance/boot-finished
              EOF
}

# --- Ansible Integration ---
resource "local_file" "ansible_inventory" {
  content = templatefile("${path.module}/inventory.ini.tpl", {
    instance_public_ip   = aws_instance.web_server.public_ip,
    ssh_private_key_path = var.ssh_private_key_path
  })
  filename = "${path.module}/../ansible/inventory.ini"
}

resource "null_resource" "ansible_provisioner" {
  depends_on = [aws_instance.web_server]

  triggers = {
    # This ensures Ansible runs if the playbook changes
    playbook_content = filemd5("${path.module}/../ansible/playbook.yml")
  }

  # This provisioner runs the Ansible playbook to deploy the Docker Compose stack
  provisioner "local-exec" {
    command = <<-EOT
      # Wait for SSH to be ready
      until nc -w 5 -z ${aws_instance.web_server.public_ip} 22; do
          echo "Waiting for SSH..."
          sleep 5
      done

      until ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i ${var.ssh_private_key_path} ec2-user@${aws_instance.web_server.public_ip} '[ -f /var/lib/cloud/instance/boot-finished ]'; do
          echo "Waiting for user_data to complete..."
          sleep 10
      done

      ansible-playbook -i ../ansible/inventory.ini ../ansible/playbook.yml --extra-vars "ghcr_username=${var.ghcr_username} ghcr_pat=${var.ghcr_pat}"
    EOT
  }
}
