# Deploying Microservices to AWS EC2 with Terraform & Ansible

This guide explains how to use the Terraform scripts in this directory to provision an AWS EC2 instance and deploy the project's microservices using Ansible.

## Prerequisites

1.  **AWS Academy Lab Access**:
    *   Ensure you have access to the AWS Academy Learner Lab.
    *   **Start your lab environment.** 

2.  **AWS Credentials**:
    *   Once your AWS Academy Lab is running, obtain your AWS access key ID, secret access key, and session token.
    *   Configure your local AWS CLI with these credentials. The simplest way is to run `aws configure` and enter the details. Alternatively, you can update your `~/.aws/credentials` file.

3.  **Software Installation**:
    *   **Terraform**: Ensure Terraform is installed on your local machine. You can download it from the [official Terraform website](https://www.terraform.io/downloads.html).
    *   **Ansible**: Ensure Ansible is installed. Installation instructions can be found on the [official Ansible documentation](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html). (e.g., `pip install ansible`)

4.  **SSH Key**:
    *   Locate the private SSH key file provided by the AWS Academy Lab. You will need its path for the Terraform configuration.

5.  **GitHub Personal Access Token (PAT)**:
    *   You'll need a GitHub PAT with `read:packages` scope to allow Ansible to pull Docker images from GitHub Container Registry (GHCR).

## Deployment Steps

1.  **Navigate to the Terraform Directory**:
    ```bash
    cd sport-matcher/terraform
    ```
    (Or, if you are already in the project root: `cd terraform`)

2.  **Create `terraform.tfvars` File**:
    *   In the `terraform` directory, create a file named `terraform.tfvars`.
    *   Add the following content, replacing the placeholder values with your actual details:

        ```tfvars
        aws_region           = "us-east-1"
        key_name             = "vockey" // Should match the key pair from AWS Academy Lab
        ssh_private_key_path = "/path/to/your/vockey.pem" // Update with the actual path to your .pem file
        ghcr_username        = "YOUR_GITHUB_USERNAME" // Your GitHub username
        ghcr_pat             = "YOUR_GHCR_PAT"      // Your GitHub PAT with read:packages scope
        ```
    *   **Important**: This `terraform.tfvars` file contains sensitive information and is ignored by Git (as defined in `terraform/.gitignore`). Do **not** commit it.

3.  **Initialize Terraform**:
    *   This command downloads the necessary providers. Run it from the `terraform` directory.
    ```bash
    terraform init
    ```

4.  **Review Terraform Plan**:
    *   This command shows you what resources Terraform will create, modify, or destroy.
    ```bash
    terraform plan
    ```
    *   Review the output carefully to ensure it matches your expectations.

5.  **Apply Terraform Configuration**:
    *   This command provisions the EC2 instance and runs the Ansible playbook to deploy the services.
    ```bash
    terraform apply
    ```
    *   You will be prompted to confirm the action. Type `yes` and press Enter.
    *   This process can take several minutes. Terraform will output the public IP address of the new EC2 instance.

## Accessing the Services

Once `terraform apply` completes successfully:

*   **Client (Frontend)**: `http://<EC2_PUBLIC_IP>:3000`
*   **User Service**: `http://<EC2_PUBLIC_IP>:8080`
*   **Location Service**: `http://<EC2_PUBLIC_IP>:8081`
*   **Messaging Service**: `http://<EC2_PUBLIC_IP>:8082`
*   **Matching Service**: `http://<EC2_PUBLIC_IP>:8083`

Replace `<EC2_PUBLIC_IP>` with the actual public IP address output by Terraform (it's also in `../ansible/inventory.ini`).
