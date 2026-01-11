#!/bin/bash
# Setup Oracle Cloud Instance for JanusLeaf Backend
# Run this ONCE on a fresh Oracle Cloud instance
# Usage: ssh into oracle instance, then: curl -sSL <raw-url> | bash
#    or: ./setup-oracle.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 Setting up Oracle Cloud Instance for JanusLeaf${NC}"
echo ""

# Detect OS
if [ -f /etc/oracle-release ]; then
    OS="oracle"
    PKG_MGR="dnf"
elif [ -f /etc/debian_version ]; then
    OS="ubuntu"
    PKG_MGR="apt"
else
    echo -e "${RED}❌ Unsupported OS${NC}"
    exit 1
fi

echo -e "${BLUE}📦 Detected OS: ${OS}${NC}"
echo ""

# Update system
echo -e "${BLUE}📦 Updating system packages...${NC}"
if [ "$OS" = "ubuntu" ]; then
    sudo apt update && sudo apt upgrade -y
else
    sudo dnf update -y
fi

# Install Docker
echo -e "${BLUE}🐳 Installing Docker...${NC}"
if [ "$OS" = "ubuntu" ]; then
    # Install Docker on Ubuntu
    sudo apt install -y docker.io docker-compose
else
    # Install Docker on Oracle Linux
    sudo dnf install -y dnf-utils
    sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
fi

# Start and enable Docker
echo -e "${BLUE}🚀 Starting Docker service...${NC}"
sudo systemctl start docker
sudo systemctl enable docker

# Add current user to docker group
echo -e "${BLUE}👤 Adding user to docker group...${NC}"
sudo usermod -aG docker $USER

# Configure firewall
echo -e "${BLUE}🔥 Configuring firewall...${NC}"
if [ "$OS" = "ubuntu" ]; then
    # Ubuntu uses iptables
    sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
    sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
    sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
    
    # Make persistent
    echo iptables-persistent iptables-persistent/autosave_v4 boolean true | sudo debconf-set-selections
    echo iptables-persistent iptables-persistent/autosave_v6 boolean true | sudo debconf-set-selections
    sudo apt install -y iptables-persistent
    sudo netfilter-persistent save
else
    # Oracle Linux uses firewalld
    sudo firewall-cmd --permanent --add-port=8080/tcp
    sudo firewall-cmd --permanent --add-port=80/tcp
    sudo firewall-cmd --permanent --add-port=443/tcp
    sudo firewall-cmd --reload
fi

# Create app directory
echo -e "${BLUE}📁 Creating application directory...${NC}"
mkdir -p ~/janusleaf

# Create .env template
echo -e "${BLUE}📝 Creating .env template...${NC}"
if [ ! -f ~/janusleaf/.env ]; then
    cat > ~/janusleaf/.env << 'EOF'
# JanusLeaf Backend Environment Variables
# Fill in your actual values!

# Supabase Database Connection
DATABASE_URL=jdbc:postgresql://db.<YOUR-PROJECT>.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<YOUR-SUPABASE-PASSWORD>

# JWT Configuration (generate a secure random string, min 32 chars)
JWT_SECRET=<YOUR-JWT-SECRET-MIN-32-CHARACTERS>
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OpenRouter API Key (for AI features)
OPENROUTER_API_KEY=<YOUR-OPENROUTER-KEY>
EOF
    echo -e "${YELLOW}⚠️  Edit ~/janusleaf/.env with your actual credentials!${NC}"
else
    echo -e "${GREEN}   .env already exists, skipping${NC}"
fi

echo ""
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANT NEXT STEPS:${NC}"
echo ""
echo -e "1. ${BLUE}Log out and back in${NC} (for docker group to take effect):"
echo -e "   exit"
echo -e "   ssh back in"
echo ""
echo -e "2. ${BLUE}Edit the .env file${NC} with your Supabase credentials:"
echo -e "   nano ~/janusleaf/.env"
echo ""
echo -e "3. ${BLUE}Open ports in Oracle Cloud Console${NC}:"
echo -e "   - Go to: Networking → Virtual Cloud Networks → Your VCN"
echo -e "   - Click: Security Lists → Default Security List"  
echo -e "   - Add Ingress Rules for TCP ports: 80, 443, 8080"
echo ""
echo -e "4. ${BLUE}Deploy from your local machine${NC}:"
echo -e "   ./scripts/deploy.sh <this-instance-ip>"
echo ""
