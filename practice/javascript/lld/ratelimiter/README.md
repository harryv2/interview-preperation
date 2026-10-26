# Rate Limiter Service

A small internal service that other services call to check if a request should be allowed. It uses a token bucket per endpoint, based on the burst and sustained values in `config.json`.

## How to run

```
npm install
npm start
npm test
```

## API

### POST /take

```
curl -X POST http://localhost:3000/take -H 'Content-Type: application/json' -d '{"endpoint":"GET /user/:id"}'
```

Request body:

- `endpoint` - the route template from `config.json`
- `clientId` - optional. If sent, each client gets its own bucket. If not, everyone shares one bucket for that endpoint.

Response when allowed:

```json
{ "accepted": true, "remainingTokens": 9, "retryAfterMs": 0 }
```

Response when the bucket is empty:

```json
{ "accepted": false, "remainingTokens": 0, "retryAfterMs": 4000 }
```

Both return 200. The caller only needs to know if the request is allowed, so a rejected request is not an error.

Other responses:

- 400 if the body or JSON is invalid
- 404 if the endpoint is not in `config.json`

The endpoint is a POST because taking a token changes state. Each call takes 1 token, as the spec asks.

### GET /health

Returns `{ "status": "ok" }`.

## Config

`config.json` has one rule per endpoint:

```json
{
  "rateLimitsPerEndpoint": [
    { "endpoint": "GET /user/:id", "burst": 10, "sustained": 6 },
    { "endpoint": "POST /login", "algorithm": "sliding-window", "limit": 5, "windowMs": 900000 }
  ]
}
```

Token bucket (the default):

- `endpoint` - route template, used as the key
- `burst` - max tokens in the bucket, and what it starts with
- `sustained` - tokens added per `windowMs`
- `algorithm` - optional, `"token-bucket"` by default
- `windowMs` - optional, `60000` (1 minute) by default

Sliding window:

- `endpoint` - route template, used as the key
- `algorithm` - must be `"sliding-window"`
- `limit` - max requests in any `windowMs`
- `windowMs` - optional, `60000` (1 minute) by default

A rule can't mix fields from both, for example `burst` on a sliding window rule is rejected.

## How it works

Each bucket starts with `burst` tokens. Every call to `/take` first adds tokens for the time passed since the last call (`sustained` per minute, never more than `burst`), then takes one if possible. There are no timers, the refill is calculated on each call. With 120 per minute, a new token is available every 0.5 seconds.

Buckets are kept in memory in an LRU cache, limited to 100k entries so memory can't grow forever.

The config is checked with Joi when the service starts. If it's invalid, the service doesn't start.

## Things to know

- A client can go over `sustained` for a short time. With burst 100 and sustained 50, a client can use 100 at once and then get 25 more in the next 30 seconds. This is how the token bucket in the spec works.
- If the LRU cache gets full, the least used bucket is removed. That client starts again with a full bucket.
- Config also supports an optional `windowMs` and a `sliding-window` algorithm. They are not required by the spec, and the default behaviour is exactly the spec's. `POST /login` in `config.json` uses it as an example: at most 5 attempts in any 15 minutes, with no extra burst.
- Package versions are pinned to work with Node 16.13.

## Limitations

- Runs as a single instance and keeps everything in memory, so a restart resets all buckets.
- Config is only read on startup.
- There is no auth. The service trusts the caller, since it only runs inside the internal network.

## Next steps

- Move bucket state to Redis so it works with more than one instance.
- Allow different limits per client (for example paid plans).
- Reload config without a restart.
- Accept an optional `cost` in `/take`, so an expensive request (for example a bulk export) can take more than 1 token.
- Add metrics for accepted and rejected requests.
