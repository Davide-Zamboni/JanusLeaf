#!/bin/bash
# Generate .env file for production deployment from secrets.properties
# Usage: ./scripts/generate-prod-env.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
SECRETS_FILE="$BACKEND_DIR/secrets.properties"
ENV_FILE="$BACKEND_DIR/.env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Generating .env file for production...${NC}"

# Check if secrets.properties exists
if [ ! -f "$SECRETS_FILE" ]; then
    echo -e "${RED}Error: secrets.properties not found at $SECRETS_FILE${NC}"
    echo "Make sure you've run 'git-crypt unlock' first."
    exit 1
fi

# Check if secrets.properties is decrypted (not binary)
if file "$SECRETS_FILE" | grep -q "data"; then
    echo -e "${RED}Error: secrets.properties appears to be encrypted.${NC}"
    echo "Run 'git-crypt unlock' to decrypt it first."
    exit 1
fi

# Generate .env file
cat > "$ENV_FILE" << 'HEADER'
# Production Environment Variables
# Generated from secrets.properties
# DO NOT COMMIT THIS FILE!
#
# Upload these to Render Dashboard > Environment Variables

HEADER

# Add Spring profile
echo "SPRING_PROFILES_ACTIVE=prod" >> "$ENV_FILE"
echo "" >> "$ENV_FILE"

# Process secrets.properties
while IFS= read -r line || [ -n "$line" ]; do
    # Skip empty lines and comments
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    
    # Extract key and value
    if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
        key="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        
        # Convert dots and hyphens to underscores and uppercase for env var format
        env_key=$(echo "$key" | tr '.-' '_' | tr '[:lower:]' '[:upper:]')
        
        # Write to .env
        echo "${env_key}=${value}" >> "$ENV_FILE"
    fi
done < "$SECRETS_FILE"

echo "" >> "$ENV_FILE"
echo "# Server port (Render sets PORT automatically)" >> "$ENV_FILE"
echo "# SERVER_PORT=\${PORT:-8080}" >> "$ENV_FILE"

echo -e "${GREEN}✓ Generated $ENV_FILE${NC}"
echo ""
echo -e "${YELLOW}Environment variables for Render:${NC}"
echo "-----------------------------------"
grep -v '^#' "$ENV_FILE" | grep -v '^$'
echo "-----------------------------------"
echo ""
echo -e "${GREEN}Copy these to Render Dashboard > Environment Variables${NC}"
