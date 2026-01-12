# JanusLeaf Backend Makefile
# Usage: make <target>

# ===================
# Instance Configuration
# ===================
INSTANCE1_IP = 158.180.228.188
INSTANCE1_KEY = secrets/oracle-ssh-1.key

INSTANCE2_IP = 80.225.83.90
INSTANCE2_KEY = secrets/oracle-ssh-2.key

SSH_USER = opc

.PHONY: help build run test deploy logs logs-last ssh health stop restart clean \
        deploy1 deploy2 logs1 logs2 logs-last1 logs-last2 ssh1 ssh2 health1 health2 \
        stop1 stop2 restart1 restart2 deploy-all health-all \
        lb-setup lb-health lb-status lb-logs lb-restart lb-test

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
	@echo "Oracle Cloud Instances:"
	@echo "  Instance 1: $(INSTANCE1_IP)"
	@echo "  Instance 2: $(INSTANCE2_IP)"
	@echo ""
	@echo "Deployment:"
	@echo "  make deploy1      - Deploy to instance 1"
	@echo "  make deploy2      - Deploy to instance 2"
	@echo "  make deploy-all   - Deploy to both instances"
	@echo ""
	@echo "Logs:"
	@echo "  make logs1        - Stream logs from instance 1"
	@echo "  make logs2        - Stream logs from instance 2"
	@echo "  make logs-last1   - Show last 100 lines from instance 1"
	@echo "  make logs-last2   - Show last 100 lines from instance 2"
	@echo ""
	@echo "SSH:"
	@echo "  make ssh1         - SSH into instance 1"
	@echo "  make ssh2         - SSH into instance 2"
	@echo ""
	@echo "Health & Status:"
	@echo "  make health1      - Check health of instance 1"
	@echo "  make health2      - Check health of instance 2"
	@echo "  make health-all   - Check health of both instances"
	@echo ""
	@echo "Stop & Restart:"
	@echo "  make stop1        - Stop app on instance 1"
	@echo "  make stop2        - Stop app on instance 2"
	@echo "  make restart1     - Restart app on instance 1"
	@echo "  make restart2     - Restart app on instance 2"
	@echo ""
	@echo "Load Balancer (nginx on Instance 1):"
	@echo "  make lb-setup     - Install & configure nginx load balancer"
	@echo "  make lb-health    - Check load balancer health"
	@echo "  make lb-status    - Show nginx status"
	@echo "  make lb-logs      - View load balancer access logs"
	@echo "  make lb-restart   - Restart nginx"
	@echo "  make lb-test      - Test load balancing (5 requests)"
	@echo ""
	@echo "After lb-setup, access API via: http://$(INSTANCE1_IP)/api/health"
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
# Instance 1 (158.180.228.188)
# ===================

deploy1:
	@echo "🚀 Deploying to Instance 1 ($(INSTANCE1_IP))..."
	SSH_KEY=$(INSTANCE1_KEY) ./scripts/deploy.sh $(INSTANCE1_IP)

logs1:
	@echo "📋 Streaming logs from Instance 1 ($(INSTANCE1_IP))..."
	SSH_KEY=$(INSTANCE1_KEY) ./scripts/logs.sh $(INSTANCE1_IP)

logs-last1:
	@echo "📋 Showing last 100 lines from Instance 1 ($(INSTANCE1_IP))..."
	SSH_KEY=$(INSTANCE1_KEY) ./scripts/logs.sh $(INSTANCE1_IP) --last 100

ssh1:
	@echo "🔗 Connecting to Instance 1 ($(INSTANCE1_IP))..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP)

health1:
	@echo "🏥 Checking health of Instance 1 ($(INSTANCE1_IP))..."
	@curl -s http://$(INSTANCE1_IP):8080/api/health | python3 -m json.tool 2>/dev/null || curl -s http://$(INSTANCE1_IP):8080/api/health

stop1:
	@echo "🛑 Stopping app on Instance 1 ($(INSTANCE1_IP))..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP) "pkill -f 'java.*app.jar' || echo 'App not running'"

restart1:
	@echo "🔄 Restarting app on Instance 1 ($(INSTANCE1_IP))..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP) '\
		pkill -f "java.*app.jar" 2>/dev/null || true; \
		sleep 2; \
		cd ~/janusleaf && source .env && nohup java -Xmx400m -Xms200m -jar app.jar > app.log 2>&1 & \
		echo "✅ App restarted"'

# ===================
# Instance 2 (80.225.83.90)
# ===================

deploy2:
	@echo "🚀 Deploying to Instance 2 ($(INSTANCE2_IP))..."
	SSH_KEY=$(INSTANCE2_KEY) ./scripts/deploy.sh $(INSTANCE2_IP)

logs2:
	@echo "📋 Streaming logs from Instance 2 ($(INSTANCE2_IP))..."
	SSH_KEY=$(INSTANCE2_KEY) ./scripts/logs.sh $(INSTANCE2_IP)

logs-last2:
	@echo "📋 Showing last 100 lines from Instance 2 ($(INSTANCE2_IP))..."
	SSH_KEY=$(INSTANCE2_KEY) ./scripts/logs.sh $(INSTANCE2_IP) --last 100

ssh2:
	@echo "🔗 Connecting to Instance 2 ($(INSTANCE2_IP))..."
	@chmod 600 $(INSTANCE2_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE2_KEY) $(SSH_USER)@$(INSTANCE2_IP)

health2:
	@echo "🏥 Checking health of Instance 2 ($(INSTANCE2_IP))..."
	@curl -s http://$(INSTANCE2_IP):8080/api/health | python3 -m json.tool 2>/dev/null || curl -s http://$(INSTANCE2_IP):8080/api/health

stop2:
	@echo "🛑 Stopping app on Instance 2 ($(INSTANCE2_IP))..."
	@chmod 600 $(INSTANCE2_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE2_KEY) $(SSH_USER)@$(INSTANCE2_IP) "pkill -f 'java.*app.jar' || echo 'App not running'"

restart2:
	@echo "🔄 Restarting app on Instance 2 ($(INSTANCE2_IP))..."
	@chmod 600 $(INSTANCE2_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE2_KEY) $(SSH_USER)@$(INSTANCE2_IP) '\
		pkill -f "java.*app.jar" 2>/dev/null || true; \
		sleep 2; \
		cd ~/janusleaf && source .env && nohup java -Xmx400m -Xms200m -jar app.jar > app.log 2>&1 & \
		echo "✅ App restarted"'

# ===================
# Both Instances
# ===================

deploy-all: deploy1 deploy2
	@echo "✅ Deployed to both instances"

health-all:
	@echo "🏥 Checking health of all instances..."
	@echo ""
	@echo "Instance 1 ($(INSTANCE1_IP)):"
	@curl -s http://$(INSTANCE1_IP):8080/api/health | python3 -m json.tool 2>/dev/null || curl -s http://$(INSTANCE1_IP):8080/api/health || echo "❌ Unreachable"
	@echo ""
	@echo "Instance 2 ($(INSTANCE2_IP)):"
	@curl -s http://$(INSTANCE2_IP):8080/api/health | python3 -m json.tool 2>/dev/null || curl -s http://$(INSTANCE2_IP):8080/api/health || echo "❌ Unreachable"

# ===================
# Load Balancer (nginx on Instance 1)
# ===================

lb-setup:
	@echo "🔧 Setting up load balancer on Instance 1 ($(INSTANCE1_IP))..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	scp -i $(INSTANCE1_KEY) scripts/setup-loadbalancer.sh $(SSH_USER)@$(INSTANCE1_IP):~/setup-loadbalancer.sh
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP) "chmod +x ~/setup-loadbalancer.sh && ~/setup-loadbalancer.sh"

lb-health:
	@echo "🏥 Checking load balancer health..."
	@curl -s http://$(INSTANCE1_IP)/lb-health | python3 -m json.tool 2>/dev/null || echo "❌ Load balancer unreachable"

lb-status:
	@echo "📊 Load balancer status on Instance 1..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP) "sudo systemctl status nginx --no-pager"

lb-logs:
	@echo "📋 Load balancer logs..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP) "sudo tail -50 /var/log/nginx/janusleaf_access.log"

lb-restart:
	@echo "🔄 Restarting load balancer..."
	@chmod 600 $(INSTANCE1_KEY) 2>/dev/null || true
	ssh -i $(INSTANCE1_KEY) $(SSH_USER)@$(INSTANCE1_IP) "sudo systemctl restart nginx"

lb-test:
	@echo "🧪 Testing load balancer (5 requests)..."
	@echo ""
	@for i in 1 2 3 4 5; do \
		echo "Request $$i:"; \
		curl -s -I http://$(INSTANCE1_IP)/api/health 2>/dev/null | grep -E "(HTTP|X-Upstream)" || echo "❌ Failed"; \
		echo ""; \
	done
