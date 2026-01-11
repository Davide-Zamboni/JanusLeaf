#!/bin/bash
# Deploy JanusLeaf Backend to Oracle Cloud Instance (JAR deployment)
# Usage: ./scripts/deploy.sh <oracle-ip> [ssh-key-path]

# Don't exit on error - we handle errors manually

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"

ORACLE_IP="${1:-}"
SSH_KEY="${2:-$BACKEND_DIR/secrets/oracle-ssh.key}"
SSH_USER="${SSH_USER:-opc}"
REMOTE_DIR="/home/${SSH_USER}/janusleaf"

# Check arguments
if [ -z "$ORACLE_IP" ]; then
    echo -e "${RED}❌ Error: Oracle instance IP required${NC}"
    echo ""
    echo "Usage: ./scripts/deploy.sh <oracle-ip> [ssh-key-path]"
    echo ""
    echo "Examples:"
    echo "  ./scripts/deploy.sh 158.180.228.188"
    echo "  ./scripts/deploy.sh 158.180.228.188 ~/.ssh/oracle-key.pem"
    echo ""
    echo "Environment variables:"
    echo "  SSH_USER - Remote user (default: opc for Oracle Linux)"
    exit 1
fi

echo -e "${BLUE}🚀 Deploying JanusLeaf Backend to Oracle Cloud${NC}"
echo -e "   Instance: ${ORACLE_IP}"
echo -e "   User: ${SSH_USER}"
echo ""

# Step 1: Generate .env file from secrets.properties
echo -e "${BLUE}🔐 Generating environment file...${NC}"
SECRETS_FILE="secrets.properties"
ENV_FILE=".env.oracle"

if [ ! -f "$SECRETS_FILE" ]; then
    echo -e "${RED}❌ Error: secrets.properties not found${NC}"
    echo "   Make sure you've run 'git-crypt unlock' first."
    exit 1
fi

# Check if secrets.properties is decrypted
if file "$SECRETS_FILE" | grep -q "data"; then
    echo -e "${RED}❌ Error: secrets.properties is encrypted${NC}"
    echo "   Run 'git-crypt unlock' to decrypt it first."
    exit 1
fi

# Generate .env file with export statements for Oracle
cat > "$ENV_FILE" << 'HEADER'
# Production Environment Variables for Oracle
# Generated from secrets.properties - DO NOT COMMIT!

export SPRING_PROFILES_ACTIVE=prod
HEADER

while IFS= read -r line || [ -n "$line" ]; do
    # Skip empty lines and comments
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    
    # Extract key and value
    if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        
        # Convert dots and hyphens to underscores and uppercase
        env_key=$(echo "$key" | tr '.-' '_' | tr '[:lower:]' '[:upper:]')
        
        # Write to .env with export
        echo "export ${env_key}=${value}" >> "$ENV_FILE"
    fi
done < "$SECRETS_FILE"

echo -e "${GREEN}   ✓ Environment file generated${NC}"

# Step 2: Build JAR locally
echo -e "${BLUE}🔨 Building JAR...${NC}"
./gradlew bootJar --quiet

# Check if build succeeded
if [ ! -f "build/libs/janusleaf-backend-1.0.0.jar" ]; then
    echo -e "${RED}❌ Build failed - JAR not found${NC}"
    exit 1
fi
echo -e "${GREEN}   ✓ Build successful${NC}"

# Step 2: Create remote directory
echo -e "${BLUE}📁 Setting up remote directory...${NC}"
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" "mkdir -p ${REMOTE_DIR}"

# Step 3: Stop existing app if running
echo -e "${BLUE}🛑 Stopping existing app...${NC}"
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" 'pgrep -f "java.*app.jar" && pkill -f "java.*app.jar" && echo "Stopped" || echo "No app was running"'
sleep 2
echo -e "${GREEN}   ✓ Done${NC}"

# Step 4: Upload .env file to Oracle
echo -e "${BLUE}🔐 Uploading environment file...${NC}"
scp -i "$SSH_KEY" "$ENV_FILE" "${SSH_USER}@${ORACLE_IP}:${REMOTE_DIR}/.env"
echo -e "${GREEN}   ✓ Environment file uploaded${NC}"

# Step 5: Copy JAR to Oracle
echo -e "${BLUE}📦 Uploading JAR to Oracle...${NC}"
scp -i "$SSH_KEY" build/libs/janusleaf-backend-1.0.0.jar "${SSH_USER}@${ORACLE_IP}:${REMOTE_DIR}/app.jar"
echo -e "${GREEN}   ✓ Upload complete${NC}"

# Cleanup local temp file
rm -f "$ENV_FILE"

# Step 6: Start the app
echo -e "${BLUE}🚀 Starting application...${NC}"
ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" << 'REMOTE_SCRIPT'
cd ~/janusleaf

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "❌ Error: .env file not found!"
    echo "   Create ~/janusleaf/.env with your credentials"
    exit 1
fi

# Load environment variables
source .env

# Start the app in background
nohup java -Xmx400m -Xms200m -jar app.jar > app.log 2>&1 &

echo "⏳ Waiting for application to start..."
sleep 15

# Check if app is running
if pgrep -f "java -jar.*app.jar" > /dev/null; then
    echo "✅ Application started!"
else
    echo "❌ Application failed to start"
    echo "📋 Last 20 lines of log:"
    tail -20 app.log
    exit 1
fi
REMOTE_SCRIPT

# Step 6: Health check
echo -e "${BLUE}🏥 Running health check...${NC}"
sleep 5
if curl -s --connect-timeout 10 "http://${ORACLE_IP}:8080/api/health" > /dev/null 2>&1; then
    echo -e "${GREEN}   ✓ Health check passed${NC}"
else
    echo -e "${YELLOW}   ⚠ Health check pending (app may still be starting)${NC}"
fi

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo -e "🔗 Your API is available at:"
echo -e "   ${BLUE}http://${ORACLE_IP}:8080/api/health${NC}"
echo ""
echo -e "📋 Useful commands (run on Oracle instance):"
echo -e "   ${YELLOW}tail -f ~/janusleaf/app.log${NC}           # View logs"
echo -e "   ${YELLOW}pkill -f 'java -jar'${NC}                  # Stop app"
echo -e "   ${YELLOW}cd ~/janusleaf && source .env && nohup java -Xmx400m -Xms200m -jar app.jar > app.log 2>&1 &${NC}  # Start app"
