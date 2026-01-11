# Deploying JanusLeaf Backend

Deploy to Oracle Cloud with Supabase as the database.

```
┌─────────────────┐       ┌─────────────────┐
│  Oracle Cloud   │       │    Supabase     │
│  (Spring Boot)  │──────▶│  (PostgreSQL)   │
└─────────────────┘       └─────────────────┘
```

---

## 🚀 Quick Start

```bash
make deploy         # Build and deploy
make logs           # View logs
make health         # Check API health
```

---

## 📋 All Commands

### Makefile Commands

| Command | Description |
|---------|-------------|
| `make deploy` | Build and deploy to Oracle |
| `make logs` | Stream logs continuously |
| `make logs-last` | Show last 100 lines |
| `make ssh` | SSH into Oracle instance |
| `make health` | Check API health |
| `make stop` | Stop the app |
| `make restart` | Restart the app |
| `make build` | Build JAR locally |
| `make run` | Run locally |
| `make test` | Run tests |

### Deploy to Different IP

```bash
ORACLE_IP=1.2.3.4 make deploy
```

---

## 🆕 First-Time Oracle Setup

### 1. Create Instance

- **Shape:** VM.Standard.E2.1.Micro (Always Free)
- **Image:** Oracle Linux 8
- **SSH Key:** Upload your public key

### 2. Configure Security List

In Oracle Cloud Console → Networking → VCN → Security Lists:

| Port | Description |
|------|-------------|
| 22 | SSH |
| 80 | HTTP |
| 443 | HTTPS |
| 8080 | API |

**Important:** Click **"Connect public subnet to internet"** in Quick Actions!

### 3. Install Java (SSH into instance)

```bash
# Create swap (for 1GB RAM)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

# Install Java 21
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.rpm
sudo rpm -ivh jdk-21_linux-x64_bin.rpm

# Open firewall
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# Create app directory
mkdir -p ~/janusleaf
```

### 4. Deploy

```bash
make deploy
```

---

## 🔧 Manual Commands (on Oracle)

SSH in first:
```bash
make ssh
```

Then:

```bash
# View logs
tail -f ~/janusleaf/app.log

# Stop app
pkill -f 'java.*app.jar'

# Start app
cd ~/janusleaf && source .env && nohup java -Xmx400m -Xms200m -jar app.jar > app.log 2>&1 &

# Check memory
free -h

# Check disk
df -h
```

---

## 🐛 Troubleshooting

### App won't start
```bash
make logs-last      # Check recent logs
make ssh            # SSH and inspect
```

### Can't connect
1. Check Security List ports (22, 8080)
2. Check firewall: `sudo firewall-cmd --list-all`
3. Check app running: `pgrep -f java`

### Out of memory
```bash
# On Oracle instance, restart with less memory
pkill -f java
cd ~/janusleaf && source .env && nohup java -Xmx300m -Xms150m -jar app.jar > app.log 2>&1 &
```

### Database errors
- Verify Supabase credentials
- Use pooler URL (port 6543) with `?prepareThreshold=0`

---

## 📊 Environment Variables

Set in `secrets.properties` (encrypted by git-crypt):

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | ✅ | Supabase JDBC URL |
| `DATABASE_USERNAME` | ✅ | Database user |
| `DATABASE_PASSWORD` | ✅ | Database password |
| `JWT_SECRET` | ✅ | JWT signing key (32+ chars) |
| `OPENROUTER_API_KEY` | ❌ | AI features |

---

## 🔐 Files (git-crypt encrypted)

| File | Description |
|------|-------------|
| `secrets.properties` | Environment variables |
| `secrets/oracle-ssh.key` | SSH private key |

Run `git-crypt unlock` after cloning to decrypt.

---

## 🌐 Adding HTTPS

With a domain, use Caddy for auto-SSL:

```bash
# On Oracle
sudo dnf install -y caddy

sudo tee /etc/caddy/Caddyfile << EOF
your-domain.com {
    reverse_proxy localhost:8080
}
EOF

sudo systemctl enable --now caddy
```
