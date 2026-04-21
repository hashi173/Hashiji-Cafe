# Deployment Guide

## Step 1: Prepare Repository for GitHub

Your project is already secured. The `application.properties` uses environment variables (`${DB_URL}`, `${DB_PASSWORD}`, etc.) and the real credentials in `application-dev.properties` are gitignored.

**Initialize and push:**

```powershell
cd d:\Unity\Projects\CoffeShopManager

git init
git add .
git commit -m "Initial commit: Hashiji Cafe full-stack application"
git branch -M main
git remote add origin https://github.com/hashi173/Hashiji-Cafe.git
git push -u origin main
```

## Step 2: Verify Nothing Sensitive Got Committed

After pushing, check that these files are NOT in the GitHub repo:
- `application-dev.properties` (contains real passwords)
- `.env`
- `uploads/`
- `target/`

## Step 3: Deploy on Render (Free)

1. Go to [render.com](https://render.com) and sign in with GitHub
2. Click **New** -> **Web Service** -> connect to `hashi173/Hashiji-Cafe`
3. Configure:
   - **Build Command**: `./mvnw clean install -DskipTests`
   - **Start Command**: `java -jar target/*.jar`
4. Add **Environment Variables**:

| Variable      | Value                                  |
|---------------|----------------------------------------|
| DB_URL        | jdbc:postgresql://YOUR_SUPABASE_URL    |
| DB_USERNAME   | postgres.XXXXX                          |
| DB_PASSWORD   | YOUR_PASSWORD                           |
| APP_PROFILE   | prod                                    |
| PORT          | 8080                                    |

5. Click **Deploy**. Render provides a public URL like `https://hashiji-cafe.onrender.com`

## Step 4: Seed Data on First Run (Optional)

If the database is empty, run once with seed flag:
- Add environment variable: `SPRING_BOOT_RUN_ARGUMENTS=--app.seed-data=true`
- After first deploy succeeds, remove this variable and redeploy

## Security Checklist

- [x] Credentials externalized to env vars (not in code)
- [x] `application-dev.properties` in `.gitignore`
- [x] Stack traces disabled in production config
- [x] BCrypt password hashing for all users
- [x] CSRF protection enabled
- [x] Role-based access control (ADMIN/STAFF)
