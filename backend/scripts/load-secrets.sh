#!/bin/bash
# Load secrets from secrets.properties into environment variables
# Usage: source scripts/load-secrets.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SECRETS_FILE="$SCRIPT_DIR/../secrets.properties"

if [ ! -f "$SECRETS_FILE" ]; then
    echo "⚠️  secrets.properties not found at $SECRETS_FILE"
    echo "   Create it from secrets.properties.example or decrypt with git-crypt"
    return 1 2>/dev/null || exit 1
fi

# Check if file is encrypted (git-crypt encrypted files start with binary data)
if file "$SECRETS_FILE" | grep -q "data"; then
    echo "🔒 secrets.properties appears to be encrypted"
    echo "   Run: git-crypt unlock"
    return 1 2>/dev/null || exit 1
fi

echo "🔓 Loading secrets from secrets.properties..."

# Read the properties file and export as environment variables
while IFS='=' read -r key value; do
    # Skip empty lines and comments
    [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]] && continue
    
    # Trim whitespace
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)
    
    # Skip if no value
    [[ -z "$value" ]] && continue
    
    # Export the variable
    export "$key"="$value"
    echo "   ✓ Loaded $key"
done < "$SECRETS_FILE"

echo "✅ Secrets loaded into environment"
