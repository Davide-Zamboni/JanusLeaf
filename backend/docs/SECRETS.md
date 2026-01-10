# Secrets Management

This project uses **git-crypt** to encrypt sensitive files in the repository. Secrets are stored in `secrets.properties` and are automatically encrypted when committed.

## Quick Start (For New Developers)

### Prerequisites

1. Install git-crypt:
   ```bash
   # macOS
   brew install git-crypt gnupg
   
   # Ubuntu/Debian
   sudo apt-get install git-crypt gnupg
   ```

2. Have a GPG key (see [GPG Key Management](#gpg-key-management) below)

### Setup

1. Clone the repository
2. Run `git-crypt unlock` to decrypt secrets
   - If you get an error, your GPG key hasn't been added yet (see [Adding New Team Members](#adding-new-team-members))
3. Verify decryption worked:
   ```bash
   cat backend/secrets.properties
   # Should show readable text, not binary garbage
   ```

### Running Locally

**Option 1: Gradle (Recommended)**
```bash
cd backend
./gradlew bootRun
```
This automatically loads `secrets.properties` into environment variables.

**Option 2: IntelliJ IDEA**
1. Install the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile)
2. Use the "JanusLeaf Application" run configuration (already configured to load secrets)

**Option 3: Manual (Terminal)**
```bash
cd backend
source scripts/load-secrets.sh
./gradlew bootRun
```

## Secrets File Format

The `secrets.properties` file contains environment variables:

```properties
# API Keys
OPENROUTER_API_KEY=sk-or-v1-xxxx

# JWT Configuration  
JWT_SECRET=your-secure-jwt-secret-min-32-chars

# Database (optional - defaults work for local dev)
# DATABASE_URL=jdbc:postgresql://localhost:5432/janusleaf
# DATABASE_USERNAME=janusleaf
# DATABASE_PASSWORD=janusleaf
```

## GPG Key Management

git-crypt uses GPG keys to encrypt/decrypt files. Each team member needs their own GPG key.

### Check for Existing GPG Key

```bash
gpg --list-secret-keys --keyid-format LONG
```

If you see output like:
```
sec   rsa4096/ABC123DEF456 2023-01-01 [SC]
      XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
uid         [ultimate] Your Name <your.email@example.com>
```

You already have a key. Your key ID is `ABC123DEF456`.

### Create a New GPG Key

```bash
gpg --full-generate-key
```

Choose these options:
- **Key type:** `1` (RSA and RSA)
- **Key size:** `4096`
- **Expiration:** `0` (does not expire)
- **Real name:** Your full name
- **Email:** Your email address
- **Passphrase:** A strong passphrase (remember it!)

### Export Your Public Key

Share this with a team member who has git-crypt access:

```bash
gpg --armor --export YOUR_KEY_ID
```

Or copy to clipboard:
```bash
gpg --armor --export YOUR_KEY_ID | pbcopy  # macOS
```

## Adding New Team Members

If you have git-crypt access, you can add new team members:

1. **Get their public key** (they'll send you a `.asc` file or paste the key)

2. **Import the key:**
   ```bash
   gpg --import path/to/their-key.asc
   # Or paste: gpg --import (then paste key, then Ctrl+D)
   ```

3. **Find their key ID:**
   ```bash
   gpg --list-keys
   # Look for their email, note the key ID on the line starting with 'pub'
   ```

4. **Add to git-crypt:**
   ```bash
   cd /path/to/JanusLeaf  # Root of monorepo
   git-crypt add-gpg-user --trusted THEIR_KEY_ID
   ```

5. **Commit and push:**
   ```bash
   git add .git-crypt/
   git commit -m "Add GPG key for NewTeamMember"
   git push
   ```

6. **Tell them to pull and unlock:**
   ```bash
   git pull
   git-crypt unlock
   ```

## Troubleshooting

### "OpenRouter API key not configured" when running locally

The secrets aren't being loaded. Try:

1. **Check if secrets.properties is decrypted:**
   ```bash
   file backend/secrets.properties
   # Should say "ASCII text", NOT "data" or "PGP"
   ```

2. **If it shows binary/encrypted, unlock git-crypt:**
   ```bash
   git-crypt unlock
   ```

3. **Run with Gradle (auto-loads secrets):**
   ```bash
   ./gradlew bootRun
   ```

### "gpg: decryption failed: No secret key"

Your GPG key hasn't been added to git-crypt:
1. Export your public key and send to a team member with access
2. They'll add your key and push
3. Pull and run `git-crypt unlock` again

### Verifying git-crypt Status

```bash
# From the JanusLeaf root directory
git-crypt status

# Should show:
# encrypted: backend/secrets.properties
# not encrypted: backend/src/...
```

### Checking What's Encrypted

```bash
git-crypt status -e  # Show only encrypted files
```

## Security Notes

- **Never** commit `secrets.properties` without git-crypt initialized
- **Never** paste secrets in chat, issues, or PRs
- **Never** add secrets to `application.yml` directly
- The `secrets.properties.example` file is safe to commit (no real secrets)
- If secrets are accidentally exposed, rotate them immediately
