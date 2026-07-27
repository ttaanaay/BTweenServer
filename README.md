# BTween Server

Kotlin + Ktor backend for the BTween app: JWT auth, public/private quotes, likes, follows.

## Tech stack

- Kotlin + Ktor (Netty engine)
- Exposed (SQL DSL) + HikariCP connection pool
- PostgreSQL (tested against Supabase's free Postgres)
- JWT auth (access + refresh tokens), BCrypt password hashing

## Environment variables

| Variable | Required | Example | Notes |
|---|---|---|---|
| `DATABASE_URL` | yes | `postgresql://postgres:pw@db.xxxx.supabase.co:5432/postgres` | Paste Supabase's connection string directly |
| `JWT_SECRET` | yes | a long random string | Generate with `openssl rand -hex 32` - never reuse or commit this |
| `PORT` | no | `8080` | Render sets this automatically |
| `JWT_ISSUER` | no | `btween-server` | |
| `JWT_ACCESS_EXPIRY_MINUTES` | no | `60` | |
| `JWT_REFRESH_EXPIRY_DAYS` | no | `30` | |
| `CORS_ALLOWED_HOST` | no | unset = allow any origin | Only matters if you add a web client later |

## Run locally

```
export DATABASE_URL="postgresql://postgres:yourpassword@db.xxxx.supabase.co:5432/postgres"
export JWT_SECRET="$(openssl rand -hex 32)"
./gradlew run
```

Server starts on `http://localhost:8080`. Tables are created automatically on first boot.

## Deploy to Render (free tier, no credit card)

1. Push this folder to a GitHub repo.
2. On Render: **New +** -> **Web Service** -> connect the repo.
3. Environment: **Docker** (Render will detect the `Dockerfile` automatically).
4. Add the environment variables above (`DATABASE_URL` and `JWT_SECRET` at minimum) under **Environment**.
5. Instance type: **Free**.
6. Deploy. Render gives you a URL like `https://btween-server.onrender.com`.

Note: on the free tier the service sleeps after 15 minutes of no traffic and takes
30-50 seconds to wake back up on the next request - the Android app should show a
loading state that accounts for this on first launch.

## API overview

All request/response bodies are JSON.

### Auth
- `POST /auth/register` - `{ username, email, password, displayName }` -> `{ accessToken, refreshToken, user }`
- `POST /auth/login` - `{ email, password }` -> same as above
- `POST /auth/refresh` - `{ refreshToken }` -> same as above

### Users
- `GET /users/{id}` - public profile (auth optional, adds `isFollowedByMe` if logged in)
- `PUT /users/me` - update own profile (auth required)
- `POST /users/{id}/follow` / `DELETE /users/{id}/follow` (auth required)
- `GET /users/{id}/quotes?limit=&offset=` - a user's quotes (private ones only visible to the owner)

### Quotes
- `GET /quotes/feed?limit=&offset=` - public feed, newest first (auth optional)
- `GET /quotes/{id}` (auth optional)
- `POST /quotes` - create (auth required)
- `PUT /quotes/{id}` - update, owner only (auth required)
- `DELETE /quotes/{id}` - owner only (auth required)
- `POST /quotes/{id}/like` / `DELETE /quotes/{id}/like` (auth required)

Send the access token as `Authorization: Bearer <accessToken>` on any authenticated route.

## Not yet built

- Rate limiting / abuse protection
- Image upload for avatars (currently just accepts an `avatarUrl` string - pair with
  something like Supabase Storage or Cloudinary's free tier later)
- Pagination cursor (currently offset-based, fine at this scale)
