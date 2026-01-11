# JanusLeaf Backend Makefile
# Usage: make <target>

# Configuration
ORACLE_IP ?= 158.180.228.188
SSH_KEY = secrets/oracle-ssh.key
SSH_USER = opc

.PHONY: help build run test deploy logs logs-last ssh health stop restart clean

# Default target
help:
	@echo "JanusLeaf Backend - Available Commands"
	@echo "======================================="
	@echo ""
	@echo "Local Development:"
	@echo "  make build        - Build the JAR file"
	@echo "  make run          - Run locally (with secrets)"
	@echo "  make test         - Run unit tests"
	@echo "  make test-all     - Run unit + integration tests"
	@echo "  make clean        - Clean build artifacts"
	@echo ""
	@echo "Database:"
	@echo "  make db-start     - Start local PostgreSQL (Docker)"
	@echo "  make db-stop      - Stop local PostgreSQL"
	@echo ""
	@echo "Deployment (Oracle Cloud):"
	@echo "  make deploy       - Build and deploy to Oracle"
	@echo "  make logs         - Stream logs from Oracle"
	@echo "  make logs-last    - Show last 100 lines of logs"
	@echo "  make ssh          - SSH into Oracle instance"
	@echo "  make health       - Check API health"
	@echo "  make stop         - Stop app on Oracle"
	@echo "  make restart      - Restart app on Oracle"
	@echo ""
	@echo "Configuration:"
	@echo "  ORACLE_IP=x.x.x.x make deploy   - Deploy to specific IP"
	@echo ""

# ===================
# Local Development
# ===================

build:
	@echo "🔨 Building JAR..."
	./gradlew bootJar

run:
	@echo "🚀 Starting locally..."
	./gradlew bootRun

test:
	@echo "🧪 Running unit tests..."
	./gradlew test

test-all:
	@echo "🧪 Running all tests..."
	./gradlew check

clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean

# ===================
# Database
# ===================

db-start:
	@echo "🐘 Starting PostgreSQL..."
	./scripts/start-db.sh

db-stop:
	@echo "🛑 Stopping PostgreSQL..."
	./scripts/stop-db.sh

# ===================
# Deployment
# ===================

deploy:
	@echo "🚀 Deploying to Oracle Cloud ($(ORACLE_IP))..."
	./scripts/deploy.sh $(ORACLE_IP)

logs:
	@echo "📋 Streaming logs from $(ORACLE_IP)..."
	./scripts/logs.sh $(ORACLE_IP)

logs-last:
	@echo "📋 Showing last 100 lines from $(ORACLE_IP)..."
	./scripts/logs.sh $(ORACLE_IP) --last 100

ssh:
	@echo "🔗 Connecting to $(ORACLE_IP)..."
	ssh -i $(SSH_KEY) $(SSH_USER)@$(ORACLE_IP)

health:
	@echo "🏥 Checking health..."
	@curl -s http://$(ORACLE_IP):8080/api/health | python3 -m json.tool 2>/dev/null || curl -s http://$(ORACLE_IP):8080/api/health

stop:
	@echo "🛑 Stopping app on $(ORACLE_IP)..."
	ssh -i $(SSH_KEY) $(SSH_USER)@$(ORACLE_IP) "pkill -f 'java.*app.jar' || echo 'App not running'"

restart:
	@echo "🔄 Restarting app on $(ORACLE_IP)..."
	ssh -i $(SSH_KEY) $(SSH_USER)@$(ORACLE_IP) '\
		pkill -f "java.*app.jar" 2>/dev/null || true; \
		sleep 2; \
		cd ~/janusleaf && source .env && nohup java -Xmx400m -Xms200m -jar app.jar > app.log 2>&1 & \
		echo "✅ App restarted"'
