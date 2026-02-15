const STORAGE_KEY = 'activation_events'
const DEDUPE_PREFIX = 'activation_once_'

function readEvents() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeEvents(events) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(events.slice(-200)))
}

export function trackActivationEvent(eventName, payload = {}, options = {}) {
  if (!eventName) return

  const { onceKey } = options
  if (onceKey) {
    const dedupeKey = `${DEDUPE_PREFIX}${onceKey}`
    if (localStorage.getItem(dedupeKey)) return
    localStorage.setItem(dedupeKey, '1')
  }

  const event = {
    eventName,
    payload,
    timestamp: new Date().toISOString()
  }

  const events = readEvents()
  events.push(event)
  writeEvents(events)

  const isTestEnv = typeof process !== 'undefined' && process?.env?.NODE_ENV === 'test'
  if (!isTestEnv) {
    console.info('[activation-event]', event)
  }

  const body = JSON.stringify(event)
  if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
    try {
      navigator.sendBeacon('/api/metrics/events', body)
      return
    } catch {
      // fall through
    }
  }

  fetch('/api/metrics/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body
  }).catch(() => {
    // analytics sink best-effort
  })
}

export function getActivationEvents() {
  return readEvents()
}
