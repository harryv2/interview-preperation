import express, { Request } from "express"
import { rateLimit } from "./middleware"

const ONE_MINUTE_MS = 60_000

const app = express()
app.use(express.json())

const clientIp = (req: Request) => req.ip ?? "unknown"
const userId = (req: Request) => req.header("x-user-id")
const userPlan = (req: Request) => req.header("x-user-plan")

app.use(rateLimit({
  algorithm: "token-bucket",
  limit: 300,
  windowMs: ONE_MINUTE_MS,
  maxKeys: 50_000,
  skip: (req) => req.path === "/health",
}))

app.post("/login", rateLimit({
  algorithm: "sliding-counter",
  limit: 5,
  windowMs: 15 * ONE_MINUTE_MS,
  key: (req) => `${clientIp(req)}:${req.body?.email ?? ""}`,
  maxKeys: 100_000,
}), (req, res) => {
  res.json({ ok: true })
})

app.use("/api", rateLimit({
  algorithm: "sliding-counter",
  limit: (req) => (userPlan(req) === "pro" ? 300 : 30),
  windowMs: 10 * ONE_MINUTE_MS,
  key: (req) => userId(req) ?? clientIp(req),
  cost: (req) => (req.path.startsWith("/export") ? 10 : 1),
}))

app.get("/api/items", (req, res) => {
  res.json({ items: [] })
})

app.get("/api/export", (req, res) => {
  res.json({ exported: true })
})

app.get("/health", (req, res) => {
  res.json({ ok: true })
})

app.listen(3000, () => {
  console.log("listening on :3000")
})
