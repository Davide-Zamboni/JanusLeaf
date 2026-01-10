#!/bin/bash
# Start the development database

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🐘 Starting PostgreSQL database..."

# Check if Colima/Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running!"
    echo ""
    echo "If you use Colima, run:"
    echo "  colima start"
    echo ""
    echo "If you use Docker Desktop, open Docker Desktop app."
    exit 1
fi

# Start the database
cd "$PROJECT_DIR"
docker-compose -f docker-compose.dev.yml up -d

echo ""
echo "✅ Database started!"
echo ""
echo "📊 Connection details:"
echo "   Host:     localhost"
echo "   Port:     5432"
echo "   Database: janusleaf"
echo "   Username: janusleaf"
echo "   Password: janusleaf"
echo ""
echo "🚀 Now run the app from IntelliJ: 'JanusLeaf Application'"
