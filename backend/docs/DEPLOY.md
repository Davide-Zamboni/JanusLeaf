# Deploying to Oracle Cloud (with Supabase)

This guide covers deploying JanusLeaf Backend to an Oracle Cloud compute instance with Supabase as the database.

## Architecture

```
┌─────────────────┐       ┌─────────────────┐
│  Oracle Cloud   │       │    Supabase     │
│    Instance     │──────▶│   PostgreSQL    │
│  (Spring Boot)  │       │                 │
└─────────────────┘       └─────────────────┘
        │
        ▼
   Your Users
```

## Prerequisites

- Oracle Cloud account with a compute instance (Ubuntu or Oracle Linux)
- Supabase project with database credentials
- SSH access to your Oracle instance
- Docker installed locally (optional, for testing)

## Quick Deploy (3 Steps)

### Step 1: Setup Oracle Instance (One-Time)

SSH into your Oracle instance and run the setup script:

```bash
ssh -i ~/.ssh/your-key.pem ubuntu@<ORACLE_IP>

# Download and run setup script
curl -sSL https://raw.githubusercontent.com/<YOUR_REPO>/backend/main/scripts/setup-oracle.sh | bash

# Or if you have the repo cloned:
./scripts/setup-oracle.sh
```

**Important:** Log out and back in after setup for Docker permissions to take effect.

### Step 2: Configure Environment

On the Oracle instance, edit the `.env` file:

```bash
nano ~/janusleaf/.env
```

Fill in your Supabase credentials:

```env
# Supabase Database (from Supabase Dashboard → Settings → Database)
DATABASE_URL=jdbc:postgresql://db.<PROJECT>.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<your-supabase-password>

# JWT Secret (generate with: openssl rand -base64 32)
JWT_SECRET=<your-secret-min-32-chars>
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OpenRouter API (optional, for AI features)
OPENROUTER_API_KEY=<your-key>
```

### Step 3: Deploy

From your local machine:

```bash
cd backend
./scripts/deploy.sh <ORACLE_IP>

# With custom SSH key:
./scripts/deploy.sh <ORACLE_IP> ~/.ssh/oracle-key.pem

# With Oracle Linux (uses 'opc' user):
SSH_USER=opc ./scripts/deploy.sh <ORACLE_IP>
```

That's it! Your API will be available at `http://<ORACLE_IP>:8080`

---

## Manual Deployment

If you prefer to deploy manually:

### 1. Copy Files to Oracle Instance

```bash
scp -i ~/.ssh/your-key.pem -r \
    Dockerfile \
    docker-compose.prod.yml \
    build.gradle.kts \
    settings.gradle.kts \
    gradle.properties \
    gradlew \
    gradle \
    src \
    ubuntu@<ORACLE_IP>:~/janusleaf/
```

### 2. Build and Run on Oracle

```bash
ssh -i ~/.ssh/your-key.pem ubuntu@<ORACLE_IP>
cd ~/janusleaf

# Create .env file with your credentials (see Step 2 above)

# Build and start
docker-compose -f docker-compose.prod.yml up -d --build

# Check logs
docker logs -f janusleaf-api
```

---

## Oracle Cloud Security List

You MUST open ports in the Oracle Cloud Console:

1. Go to **Networking → Virtual Cloud Networks**
2. Click your VCN → **Security Lists** → **Default Security List**
3. Click **Add Ingress Rules**
4. Add rules for:
   - **Port 8080** (API) - Source CIDR: `0.0.0.0/0`
   - **Port 80** (HTTP) - Source CIDR: `0.0.0.0/0`
   - **Port 443** (HTTPS) - Source CIDR: `0.0.0.0/0`

---

## Adding HTTPS (Recommended)

For production, add SSL with Caddy (easiest auto-SSL):

```bash
# On Oracle instance
sudo apt install -y caddy

# Create Caddyfile
sudo tee /etc/caddy/Caddyfile << EOF
your-domain.com {
    reverse_proxy localhost:8080
}
EOF

sudo systemctl restart caddy
```

Caddy automatically obtains and renews Let's Encrypt certificates!

---

## Useful Commands

```bash
# View logs
docker logs -f janusleaf-api

# Restart application
docker restart janusleaf-api

# Stop application
docker-compose -f docker-compose.prod.yml down

# Rebuild and redeploy
docker-compose -f docker-compose.prod.yml up -d --build

# Check container status
docker ps

# Check health
curl http://localhost:8080/api/health
```

---

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs

# Common issues:
# - Missing .env file
# - Wrong Supabase credentials
# - Port already in use
```

### Can't connect from internet

1. Check Oracle Security List (must have ingress rules for port 8080)
2. Check iptables on instance: `sudo iptables -L -n`
3. Check if container is running: `docker ps`

### Database connection failed

1. Verify Supabase credentials in `.env`
2. Check if Supabase allows connections from your Oracle IP
3. Test connection: `curl -v telnet://db.<project>.supabase.co:5432`

### Health check failing

```bash
# Check if app started
docker logs janusleaf-api | head -50

# Common issues:
# - Database migrations failing
# - Invalid JWT secret (must be 32+ chars)
```

---

## Environment Variables Reference

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | ✅ | Supabase JDBC URL |
| `DATABASE_USERNAME` | ✅ | Database username (usually `postgres`) |
| `DATABASE_PASSWORD` | ✅ | Database password |
| `JWT_SECRET` | ✅ | Secret for signing JWTs (min 32 chars) |
| `JWT_ACCESS_EXPIRATION` | ❌ | Access token lifetime in ms (default: 900000 = 15min) |
| `JWT_REFRESH_EXPIRATION` | ❌ | Refresh token lifetime in ms (default: 604800000 = 7 days) |
| `OPENROUTER_API_KEY` | ❌ | OpenRouter API key for AI features |
