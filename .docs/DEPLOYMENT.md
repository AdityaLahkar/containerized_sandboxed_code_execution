# SandCode Deployment Guide 🚀

This guide walks you through deploying the SandCode REST API to a production Linux Virtual Private Server (VPS). 

> [!IMPORTANT]
> **Why a VPS and not a free App Hosting platform?**
> SandCode explicitly relies on host-level `docker run` commands to execute untrusted code in isolated, ephemeral containers. Free platforms like Render, Heroku, or Vercel use strict "containerized" runtimes that explicitly block Docker-in-Docker execution. You **must** deploy this on a Linux VM where you have root access.

## Prerequisites
- A cloud VPS provider (Recommended: **Oracle Cloud "Always Free" Tier** ARM instance with 24GB RAM, or an AWS EC2 `t2.micro`).
- OS: **Ubuntu 22.04 LTS** or **24.04 LTS**.
- SSH access to your server.

---

## Step 1: Install System Dependencies

Connect to your VPS via SSH and install Docker, Java 21, and Git:

```bash
# Update package list
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install docker.io -y
sudo systemctl enable --now docker

# Give your user permission to run docker commands (so Spring Boot runs without sudo)
sudo usermod -aG docker $USER
newgrp docker

# Install Java 21 (Amazon Corretto or OpenJDK)
sudo apt install openjdk-21-jdk -y

# Install Git
sudo apt install git -y
```

> [!NOTE]
> Run `docker ps` to verify Docker is running without requiring `sudo`.

---

## Step 2: Clone and Build the Application

Clone your repository to the server and build the `.jar` executable using Maven.

```bash
# Clone the repository
git clone https://github.com/AdityaLahkar/containerized_sandboxed_code_execution.git
cd containerized_sandboxed_code_execution/code

# Build the Spring Boot executable
./mvnw clean package -DskipTests
```

The compiled binary is now located at `target/code-0.0.1-SNAPSHOT.jar`.

---

## Step 3: Pre-Pull Language Docker Images

Whenever `ExecutorService.java` spins up a container, it requests a specific Docker image. If the image doesn't exist on the host, Docker will attempt to pull it dynamically, causing a massive initial request timeout.

Pre-pull all supported language images explicitly:

```bash
# For the CStrategy
docker pull gcc:latest

# Add other images here as you expand V2! (e.g., docker pull python:3.11-alpine)
```

---

## Step 4: Run as a Background Service (Systemd)

If you simply run `java -jar`, the server will shut down when you close your SSH terminal. To make SandCode run in the background gracefully, restart automatically on crashes, and survive server reboots, configure it as a Linux service.

Create a new service file:
```bash
sudo nano /etc/systemd/system/sandcode.service
```

Paste the following configuration (replace `ubuntu` with your actual Linux `$USER`):
```ini
[Unit]
Description=SandCode Spring Boot API
After=network.target docker.service
Requires=docker.service

[Service]
User=ubuntu
Group=docker
# Path to the directory where the jar lives
WorkingDirectory=/home/ubuntu/containerized_sandboxed_code_execution/code
# Path to the compiled jar
ExecStart=/usr/bin/java -jar target/code-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Save (`Ctrl+O`, `Enter`) and Exit (`Ctrl+X`).

**Enable and Start the Service:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable sandcode
sudo systemctl start sandcode

# Verify it's running
sudo systemctl status sandcode
```
Your API is now running locally on port `8080`.

---

## Step 5: (Optional) Expose to the Internet

By default, Spring Boot runs on `localhost:8080`. To expose your slick UI and API to the internet safely on port `80` (HTTP), set up **Caddy Server** (which also handles automatic HTTPS if you connect a domain name!).

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install caddy
```

Point Caddy to your background Spring Boot app:
```bash
sudo nano /etc/caddy/Caddyfile
```

Replace the contents with:
```text
:80 {
    reverse_proxy localhost:8080
}
```

Restart Caddy:
```bash
sudo systemctl restart caddy
```

🎉 **Congratulations!** SandCode is now live securely on your free VPS server!
