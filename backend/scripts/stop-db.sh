#!/bin/bash
# Stop the development database

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"

echo "🛑 Stopping PostgreSQL database..."

cd "$BACKEND_DIR"
docker-compose -f docker-compose.dev.yml down

echo "✅ Database stopped!"
