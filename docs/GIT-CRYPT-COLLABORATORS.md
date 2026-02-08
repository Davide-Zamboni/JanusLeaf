# Adding a Collaborator to git-crypt

This repo uses **git-crypt** to encrypt sensitive files (e.g. `backend/secrets.properties`, keys under `backend/secrets/`). Only people whose GPG key is added can run `git-crypt unlock` and decrypt them.

Only someone who **already has git-crypt access** can add a new collaborator.

---

## Prerequisites (for the new collaborator)

The new collaborator must:

1. **Install git-crypt and GPG**
   - macOS: `brew install git-crypt gnupg`
   - Linux: `sudo apt-get install git-crypt gnupg` (or equivalent)

2. **Create a GPG key** (if they don’t have one):
   ```bash
   gpg --full-generate-key
   ```
   - Key type: RSA and RSA  
   - Key size: 4096  
   - Expiration: 0 (no expiry)  
   - Real name, email, and a strong passphrase

3. **Export their public key** and send it to you (e.g. as a `.asc` file or pasted text):
   ```bash
   gpg --armor --export THEIR_EMAIL@example.com
   ```
   Or copy to clipboard on macOS: `gpg --armor --export THEIR_EMAIL@example.com | pbcopy`

---

## Steps for an existing collaborator (adding someone new)

Do this from the **root of the JanusLeaf repo** (where `.git-crypt/` lives).

### 1. Import the collaborator’s public key

```bash
gpg --import path/to/their-key.asc
```

If they pasted the key: run `gpg --import`, paste the key, then press Ctrl+D.

### 2. (Optional) Sign the key to mark it trusted

```bash
gpg --lsign-key their-email@example.com
```

### 3. Get their key ID

```bash
gpg --list-keys
```

Find the line starting with `pub` for their key; the key ID is the last part (e.g. `A1B2C3D4E5F6...`). You can use the full fingerprint or the short key ID.

### 4. Add them to git-crypt

```bash
git-crypt add-gpg-user --trusted THEIR_KEY_ID
```

Replace `THEIR_KEY_ID` with their key ID or email (e.g. `alice@example.com`).

### 5. Commit and push the updated keys

```bash
git add .git-crypt/
git commit -m "Add GPG key for [Collaborator Name]"
git push
```

### 6. Tell the new collaborator

They should:

```bash
git pull
git-crypt unlock
```

They’ll be prompted for their GPG passphrase once; after that, encrypted files will be decrypted in their working tree.

---

## Quick reference

| Role              | Action |
|-------------------|--------|
| **New collaborator** | Install git-crypt + GPG, create GPG key, export public key, send it to an existing collaborator. After being added: `git pull` then `git-crypt unlock`. |
| **Existing collaborator** | Import their key → (optional) sign key → `git-crypt add-gpg-user --trusted THEIR_KEY_ID` → commit `.git-crypt/` → push. |

---

## More details

- Full setup, troubleshooting, and secret management: **[backend/docs/SECRETS.md](../backend/docs/SECRETS.md)**  
- Which files are encrypted: see **`.gitattributes`** in the repo root.
