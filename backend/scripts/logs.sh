#!/bin/bash
# View JanusLeaf logs from Oracle Cloud Instance
# Usage: ./scripts/logs.sh <oracle-ip> [ssh-key-path]
#        ./scripts/logs.sh <oracle-ip> --last 100   # Show last 100 lines

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"

ORACLE_IP="${1:-}"
SSH_KEY="${2:-$BACKEND_DIR/secrets/oracle-ssh.key}"
SSH_USER="${SSH_USER:-opc}"
REMOTE_DIR="/home/${SSH_USER}/janusleaf"

# Check for --last flag
LINES=""
if [[ "$2" == "--last" ]]; then
    LINES="${3:-50}"
fi

# Check arguments
if [ -z "$ORACLE_IP" ]; then
    echo "❌ Error: Oracle instance IP required"
    echo ""
    echo "Usage:"
    echo "  ./scripts/logs.sh <oracle-ip>              # Stream logs continuously"
    echo "  ./scripts/logs.sh <oracle-ip> --last 100   # Show last 100 lines"
    echo ""
    echo "Examples:"
    echo "  ./scripts/logs.sh 158.180.228.188"
    echo "  ./scripts/logs.sh 158.180.228.188 --last 50"
    exit 1
fi

echo "📋 Connecting to JanusLeaf logs at ${ORACLE_IP}..."
echo "   Press Ctrl+C to stop"
echo ""

if [ -n "$LINES" ]; then
    # Show last N lines
    ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" "tail -n ${LINES} ${REMOTE_DIR}/app.log"
else
    # Stream logs continuously
    ssh -i "$SSH_KEY" "${SSH_USER}@${ORACLE_IP}" "tail -f ${REMOTE_DIR}/app.log"
fi
