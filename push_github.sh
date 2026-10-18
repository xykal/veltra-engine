#!/usr/bin/env bash
# VELTRA — buat repo GitHub (PUBLIC) + push skeleton pertama
#
# CARA PAKAI:
#   export GITHUB_TOKEN=ghp_xxxxxxxxxxxx      # jangan pernah di-commit!
#   ./push_github.sh <username> [repo-name]
#
# Token: Fine-grained PAT → scope "Contents: Read & write" + "Actions: Read & write"
#        khusus 1 repo ini aja. Set expiry 30 hari.
set -euo pipefail

USERNAME="${1:?isi username GitHub, contoh: ./push_github.sh adiibnu veltra-engine}"
REPO="${2:-veltra-engine}"
VISIBILITY="public"
BRANCH="main"

if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "ERROR: GITHUB_TOKEN belum di-set."
  echo "       export GITHUB_TOKEN=xxx  lalu ulangi."
  exit 1
fi

# 1. Buat repo via GitHub API (kalau belum ada)
echo "→ cek/ buat repo https://github.com/${USERNAME}/${REPO} ..."
HTTP=$(curl -s -o /tmp/gh_resp.json -w "%{http_code}" \
  -X POST https://api.github.com/user/repos \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -d "{\"name\":\"${REPO}\",\"description\":\"VELTRA — Game Performance System. All device, all games. Native Android (Kotlin + Compose).\",\"homepage\":\"https://github.com/${USERNAME}/${REPO}\",\"private\":false,\"has_issues\":true,\"has_wiki\":false,\"auto_init\":false}")
echo "   status: ${HTTP}"
grep -q '"full_name"' /tmp/gh_resp.json && echo "   ✓ repo siap" || true

# 2. Init & commit
[ -d .git ] || git init -b "$BRANCH"
git add -A
git -c user.name="VELTRA Bot" -c user.email="bot@veltra.local" \
  commit -m "feat: VELTRA skeleton — launcher, overlay HUD, boost engine, CI" || echo "   (tidak ada perubahan baru)"

# 3. Push
git remote remove origin 2>/dev/null || true
git remote add origin "https://${USERNAME}:${GITHUB_TOKEN}@github.com/${USERNAME}/${REPO}.git"
echo "→ push ke ${VISIBILITY} repo..."
git push -u origin "$BRANCH"

echo ""
echo "✓ SELESAI"
echo "  Repo   : https://github.com/${USERNAME}/${REPO}"
echo "  Actions: https://github.com/${USERNAME}/${REPO}/actions"
echo ""
echo "⚠️  Setelah selesai: REVOKE token di github.com/settings/tokens"
