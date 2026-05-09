# Luciano — Setup Guide

Welcome to Tatalance. This guide gets you from zero to running the app locally and deploying to your cloud environment.

---

## Prerequisites

You need: Windows 11 ARM64, WSL2, IntelliJ IDEA, a GitHub account added as collaborator on `drompincen/tatalance`.

---

## Step 1 — Clone the repo

In WSL2:

```bash
git clone https://github.com/drompincen/tatalance.git
cd tatalance
```

---

## Step 2 — Wire `mvn` to Windows Maven

This is the most important step. Maven must run as a **Windows process** — if it runs as Linux, the embedded MongoDB won't start locally.

```bash
# Check if you already have Windows Maven
mvn -version
# Good output: "Maven home: C:\..."
# Bad output: "Maven home: /usr/..."
```

If the output shows a Windows path, skip to Step 3.

If not, set it up:

```bash
# Find your Windows Maven
find /mnt/c/Users/$USER/.m2/wrapper/dists -name "mvn.cmd" 2>/dev/null

# Create the wrapper (replace the path with your output above)
mkdir -p ~/.local/bin
cat > ~/.local/bin/mvn << 'EOF'
#!/bin/sh
exec cmd.exe /c "C:\\Users\\luciano\\.m2\\wrapper\\dists\\apache-maven-3.9.9-bin\\<hash>\\apache-maven-3.9.9\\bin\\mvn.cmd" "$@"
EOF
chmod +x ~/.local/bin/mvn

# Verify
mvn -version   # must show "Maven home: C:\..."
```

If you don't have Windows Maven yet, open IntelliJ IDEA → open the `backend/` folder as a Maven project. IntelliJ will download Maven automatically. Then run the `find` command above.

---

## Step 3 — Run the app locally

```bash
cd backend
mvn spring-boot:run
```

Open: `http://localhost:8080/index.html`

The app uses an in-memory MongoDB (Flapdoodle) locally — no database setup needed. Data is wiped on restart.

---

## Step 4 — Set up AWS CLI (for CloudWatch logs)

Install AWS CLI in WSL2:

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
aws --version
```

Configure with your credentials (get these from drom):

```bash
aws configure --profile luciano-dev
# AWS Access Key ID:     AKIAQUE2DN2D57DBILZX
# AWS Secret Access Key: <ask drom for this>
# Default region:        us-east-1
# Output format:         json
```

Verify:

```bash
aws sts get-caller-identity --profile luciano-dev
```

---

## Step 5 — Check your cloud environment

Your EB environment: `http://tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com/index.html`

Check its health:

```bash
aws elasticbeanstalk describe-environments \
  --application-name tatalance \
  --environment-names tatalance-luciano \
  --query "Environments[0].{Status:Status,Health:Health}" \
  --profile luciano-dev
```

Pull your app logs:

```bash
aws logs get-log-events \
  --log-group-name "/aws/elasticbeanstalk/tatalance-luciano/var/log/web.stdout.log" \
  --log-stream-name $(aws logs describe-log-streams \
    --log-group-name "/aws/elasticbeanstalk/tatalance-luciano/var/log/web.stdout.log" \
    --order-by LastEventTime --descending \
    --query "logStreams[0].logStreamName" --output text \
    --profile luciano-dev) \
  --limit 50 \
  --query "events[*].message" --output text \
  --profile luciano-dev | tail -30
```

---

## Step 6 — Daily development workflow

```bash
# Start of day — sync your branch
git checkout luciano
git pull origin luciano

# Make your changes, then push to trigger your EB deploy
git add <files>
git commit -m "feat: what I did (#issue-number)"
git push origin luciano

# Watch the deploy in GitHub Actions
# https://github.com/drompincen/tatalance/actions

# When ready to share with drom — open a PR to main
gh pr create --base main --title "feat: your feature" --body "Closes #<issue>"
```

---

## Step 7 — Using Claude Code

Start Claude in WSL2 from the repo root:

```bash
cd tatalance
claude
```

Tell Claude which issue you're working on at the start of each session. Claude reads the codebase and helps you implement, debug, and review. Always pull latest before starting so Claude has the current code.
