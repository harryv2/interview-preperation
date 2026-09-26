type CacheEntry<V> = {
  value: V
  expiresAt: number
}

export interface LruTtlCacheOptions {
  maxEntries: number
  ttlMs: number
  cleanupIntervalMs?: number
}

export class LruTtlCache<K, V> {
  private readonly entries = new Map<K, CacheEntry<V>>()
  private readonly maxEntries: number
  private readonly ttlMs: number
  private readonly cleanupTimer: NodeJS.Timeout

  constructor({ maxEntries, ttlMs, cleanupIntervalMs = ttlMs }: LruTtlCacheOptions) {
    if (maxEntries <= 0) {
      throw new Error("maxEntries must be > 0")
    }
    if (ttlMs <= 0) {
      throw new Error("ttlMs must be > 0")
    }
    if (cleanupIntervalMs <= 0) {
      throw new Error("cleanupIntervalMs must be > 0")
    }

    this.maxEntries = maxEntries
    this.ttlMs = ttlMs

    this.cleanupTimer = setInterval(() => {
      this.removeExpired(Date.now())
    }, cleanupIntervalMs)
    this.cleanupTimer.unref()
  }

  stopCleanup(): void {
    clearInterval(this.cleanupTimer)
  }

  get(key: K): V | undefined {
    const now = Date.now()
    this.removeExpired(now)

    const entry = this.entries.get(key)
    if (!entry) {
      return undefined
    }

    this.entries.delete(key)
    entry.expiresAt = now + this.ttlMs
    this.entries.set(key, entry)

    return entry.value
  }

  set(key: K, value: V): void {
    const now = Date.now()
    this.removeExpired(now)

    this.entries.delete(key)
    if (this.entries.size >= this.maxEntries) {
      this.removeLeastRecentlyUsed()
    }

    this.entries.set(key, { value, expiresAt: now + this.ttlMs })
  }

  delete(key: K): boolean {
    return this.entries.delete(key)
  }

  get size(): number {
    return this.entries.size
  }

  // Map keeps insertion order, and every access re-inserts with the same TTL,
  // so entries are sorted by expiry. The first live entry ends the scan.
  private removeExpired(now: number): void {
    for (const [key, entry] of this.entries) {
      if (entry.expiresAt > now) {
        break
      }
      this.entries.delete(key)
    }
  }

  private removeLeastRecentlyUsed(): void {
    const oldest = this.entries.keys().next()
    if (!oldest.done) {
      this.entries.delete(oldest.value)
    }
  }
}
