#!/bin/bash
# Deploy JanusLeaf Backend to Oracle Cloud Instance
# Usage: ./scripts/deploy.sh <oracle-ip> [ssh-key-path]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ORACLE_IP="${1:-}"
SSH_KEY="${2:-~/.ssh/id_rsa}"
SSH_USER="${SSH_USER:-ubuntu}"
REMOTE_DIR="/home/${SSH_USER}/janusleaf"
APP_NAME="janusleaf-api"

# Check arguments
if [ -z "$ORACLE_IP" ]; then
    echo -e "${RED}❌ Error: Oracle instance IP required${NC}"
    echo ""
    echo "Usage: ./scripts/deploy.sh <oracle-ip> [ssh-key-path]"
    echo ""
    echo "Examples:"
    echo "  ./scripts/deploy.sh 129.153.xx.xx"
    echo "  ./scripts/deploy.sh 129.153.xx.xx ~/.ssh/oracle-key.pem"
    echo ""
    echo "Environment variables:"
    echo "  SSH_USER - Remote user (default: ubuntu, use 'opc' for Oracle Linux)"
    exit 1
fi

echo -e "${BLUE}🚀 Deploying JanusLeaf Backend to Oracle Cloud${NC}"
echo -e "   Instance: ${ORACLE_IP}"
echo -e "   User: ${SSH_USER}"
echo ""

# Check if .env.prod exists locally for reference
if [ ! -f ".env.prod" ]; then
    echo -e "${YELLOW}⚠️  No .env.prod file found locally${NC}"
    echo -e "   Make sure you have .env configured on the server!"
    echo ""
fi

# Step 1: Create remote directory structure
echo -e "${BLUE}📁 Setting up remote directory...${NC}"
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" "mkdir -p ${REMOTE_DIR}"

# Step 2: Copy deployment files
echo -e "${BLUE}📦 Copying deployment files...${NC}"
scp -i "$SSH_KEY" \
    Dockerfile \
    docker-compose.prod.yml \
    build.gradle.kts \
    settings.gradle.kts \
    gradle.properties \
    gradlew \
    "${SSH_USER}@${ORACLE_IP}:${REMOTE_DIR}/"

# Copy gradle wrapper
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" "mkdir -p ${REMOTE_DIR}/gradle/wrapper"
scp -i "$SSH_KEY" \
    gradle/wrapper/gradle-wrapper.jar \
    gradle/wrapper/gradle-wrapper.properties \
    "${SSH_USER}@${ORACLE_IP}:${REMOTE_DIR}/gradle/wrapper/"

# Copy source code
echo -e "${BLUE}📦 Copying source code...${NC}"
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" "mkdir -p ${REMOTE_DIR}/src"
scp -i "$SSH_KEY" -r src/main "${SSH_USER}@${ORACLE_IP}:${REMOTE_DIR}/src/"

# Step 3: Copy .env.prod if it exists
if [ -f ".env.prod" ]; then
    echo -e "${BLUE}🔐 Copying environment file...${NC}"
    scp -i "$SSH_KEY" .env.prod "${SSH_USER}@${ORACLE_IP}:${REMOTE_DIR}/.env"
fi

# Step 4: Build and deploy on remote
echo -e "${BLUE}🔨 Building and deploying on Oracle instance...${NC}"
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" << 'REMOTE_SCRIPT'
cd ~/janusleaf

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "❌ Error: .env file not found!"
    echo "   Create .env with your Supabase and JWT credentials"
    exit 1
fi

# Check Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not installed. Run setup script first."
    exit 1
fi

# Stop existing container if running
echo "🛑 Stopping existing container..."
docker-compose -f docker-compose.prod.yml down 2>/dev/null || true

# Build and start
echo "🔨 Building Docker image..."
docker-compose -f docker-compose.prod.yml build --no-cache

echo "🚀 Starting container..."
docker-compose -f docker-compose.prod.yml up -d

# Wait for health check
echo "⏳ Waiting for application to start..."
sleep 10

# Check status
if docker ps | grep -q janusleaf-api; then
    echo "✅ Deployment successful!"
    echo ""
    echo "📊 Container status:"
    docker ps --filter name=janusleaf-api --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    echo "❌ Deployment failed!"
    echo "📋 Container logs:"
    docker-compose -f docker-compose.prod.yml logs --tail=50
    exit 1
fi
REMOTE_SCRIPT

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo -e "🔗 Your API is available at:"
echo -e "   http://${ORACLE_IP}:8080/api/health"
echo ""
echo -e "📋 Useful commands (run on Oracle instance):"
echo -e "   ${YELLOW}docker logs -f janusleaf-api${NC}     # View logs"
echo -e "   ${YELLOW}docker restart janusleaf-api${NC}     # Restart app"
echo -e "   ${YELLOW}docker-compose -f docker-compose.prod.yml down${NC}  # Stop app"
