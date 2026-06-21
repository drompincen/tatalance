# Troubleshooting Guide

---

## Local Development

### App won't start — Flapdoodle error

**Symptom:** `could not resolve package for Platform{operatingSystem=Linux, architecture=ARM_64}`

**Cause:** Maven is running as a native Linux process. It must run as a Windows process.

**Fix:** Check `mvn -version` — output must show `Maven home: C:\...`. If it shows `/usr/...`, follow the WSL2 Maven wrapper setup in `docs/luciano-setup.md` Step 2.

---

### App won't start — `IllegalStateException: Set the de.flapdoodle.mongodb.embedded.version property`

**Fix:** Add this to `backend/src/main/resources/application.yml`:

```yaml
de:
  flapdoodle:
    mongodb:
      embedded:
        version: 6.0.5
```

---

### `mvn` command not found in WSL2

**Fix:** Install Maven wrapper pointing to Windows Maven. See `docs/luciano-setup.md` Step 2.

---

## Cloud (Elastic Beanstalk)

### 502 Bad Gateway

The app is not running on the EB instance. Check the logs:

```bash
aws logs get-log-events \
  --log-group-name "/aws/elasticbeanstalk/tatalance-luciano/var/log/web.stdout.log" \
  --log-stream-name $(aws logs describe-log-streams \
    --log-group-name "/aws/elasticbeanstalk/tatalance-luciano/var/log/web.stdout.log" \
    --order-by LastEventTime --descending \
    --query "logStreams[0].logStreamName" --output text \
    --profile luciano-dev) \
  --limit 50 --query "events[*].message" --output text \
  --profile luciano-dev | tail -30
```

Common causes:
- App crashed on startup — look for `ERROR` or `Exception` in logs
- Wrong port — app must bind to port `5000` (set via `SERVER_PORT=5000` env var in EB)
- MongoDB connection failed — check Atlas network access and connection string

---

### Pipeline failing

1. Go to GitHub → Actions tab
2. Click the failed run
3. Click the failed step to expand logs
4. Common failures:
   - `No Environment found` — EB environment not Ready yet, re-run the workflow
   - `InvalidClientTokenId` — AWS credentials expired or wrong profile
   - `Build failure` — Java compile error, fix the code first

---

### MongoDB not connecting

**Check 1** — Is the `MONGODB_URI` set in EB?

```bash
aws elasticbeanstalk describe-configuration-settings \
  --application-name tatalance \
  --environment-name tatalance-luciano \
  --query "ConfigurationSettings[0].OptionSettings[?OptionName=='SPRING_DATA_MONGODB_URI']" \
  --profile luciano-dev
```

**Check 2** — Is `0.0.0.0/0` in Atlas Network Access?
Atlas → Network Access → confirm `0.0.0.0/0` is listed.

**Check 3** — Is the Atlas user `tatalance-luciano` active?
Atlas → Database Access → confirm user exists and is not locked.

---

### Google sign-in / Link Google does not work

**Symptom:** "Sign in with Google" on the login page does nothing, shows **Unauthorized**, or **Link Google** in the header fails.

**Cause:** Google OAuth is only active when both environment variables are set on the Elastic Beanstalk environment:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Without them, Spring Security does not register the OAuth routes and `/oauth2/authorization/google` returns **401**. The UI now hides Google buttons when OAuth is disabled (`/api/info` → `googleOAuthEnabled: false`).

**Fix (drom / admin):**

1. [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials → OAuth 2.0 Client ID (Web application).
2. Authorized redirect URIs must include each EB hostname, e.g.  
   `http://tatalance-luciano.eba-7u2dj39y.us-east-1.elasticbeanstalk.com/login/oauth2/code/google`  
   (repeat for `tatalance-qa`, `tatalance-prod`, and `http://localhost:8080/...` for local dev).
3. EB → Environment → Configuration → Software → Environment properties → add `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
4. Restart the environment; confirm `GET /api/info` returns `"googleOAuthEnabled": true`.

**Link Google flow:** Log in with username/password first, then click **Link Google** — it attaches your Google account to the existing user (issue #72).

---

### Check environment health

```bash
aws elasticbeanstalk describe-environments \
  --application-name tatalance \
  --query "Environments[*].{Name:EnvironmentName,Status:Status,Health:Health}" \
  --output table \
  --profile luciano-dev
```

---

## GitHub / Git

### Can't push directly to `main`

**Expected** — main is protected. Open a PR from your branch instead:

```bash
gh pr create --base main --title "your title" --body "Closes #<issue>"
```

### Merge conflict when pulling

```bash
git checkout luciano
git fetch origin
git rebase origin/main   # rebase your branch on top of latest main
# Fix any conflicts, then:
git rebase --continue
git push origin luciano --force-with-lease
```
