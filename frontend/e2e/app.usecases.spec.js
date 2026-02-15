const { test, expect } = require('@playwright/test')

function makeHistoryData(points = 60) {
  const data = []
  for (let index = 0; index < points; index += 1) {
    const day = String((index % 28) + 1).padStart(2, '0')
    data.push({
      timestamp: `2020-01-${day}`,
      open: 100 + index,
      high: 101 + index,
      low: 99 + index,
      close: 100 + index
    })
  }
  return data
}

async function bootstrapLoginViaUI(page, request) {
  const username = `pw_${Date.now()}_${Math.floor(Math.random() * 10000)}`
  const password = 'Pass123!'

  const registerResponse = await request.post('http://localhost:8080/api/auth/register', {
    data: { username, password }
  })
  expect([200, 409]).toContain(registerResponse.status())

  await page.goto('/')
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'Login' })).toBeVisible()
  await page.locator('label:has-text("Username") + input').fill(username)
  await page.locator('label:has-text("Password") + input').fill(password)

  const loginResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login') && response.request().method() === 'POST'
  )

  await page.getByRole('button', { name: 'Login' }).click()

  const loginResponse = await loginResponsePromise
  expect(loginResponse.status()).toBe(200)

  await page.goto('/')
  await expect(page.getByText(`Signed in as ${username}`)).toBeVisible()

  return { username }
}

async function createWatchlist(page, name = 'E2E List') {
  const createNewWatchlistButton = page.getByLabel('Create new watchlist')
  if (await createNewWatchlistButton.count()) {
    await createNewWatchlistButton.click()
  } else {
    await page.getByRole('button', { name: 'Create Your First Watchlist' }).click()
  }

  await page.getByPlaceholder('Watchlist name').fill(name)
  await page.getByRole('button', { name: 'Create', exact: true }).click()
}

async function addSymbol(page, symbol) {
  await page.getByLabel('Add symbol').click()
  await page.getByPlaceholder('Stock symbol (e.g., AAPL, MSFT)').fill(symbol)
  await page.getByRole('button', { name: 'Add', exact: true }).click()
}

test.describe('Trading app browser use cases', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/stocks/*/price', async (route) => {
      const url = route.request().url()
      const symbol = url.split('/api/stocks/')[1].split('/price')[0]
      const prices = { AAPL: 100.11, MSFT: 300.22, TSLA: 200.33, NVDA: 400.44 }
      const price = prices[symbol] || 150.55
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          symbol,
          price,
          high: price * 1.02,
          low: price * 0.98,
          date: '2026-02-14'
        })
      })
    })

    await page.route('**/api/stocks/*/history**', async (route) => {
      const url = route.request().url()
      const symbol = url.split('/api/stocks/')[1].split('/history')[0]
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          symbol,
          interval: 'daily',
          data: makeHistoryData(60)
        })
      })
    })
  })

  test('ui login + watchlist create/rename/switch/delete flows', async ({ page, request }) => {
    const listA = `E2E List A ${Date.now()}`
    const listB = `E2E List B ${Date.now()}`
    const listRenamed = `E2E List A Renamed ${Date.now()}`

    await bootstrapLoginViaUI(page, request)
    await createWatchlist(page, listA)

    page.once('dialog', (dialog) => dialog.accept(listRenamed))
    await page.getByLabel('Rename watchlist').click()

    const watchlistSelect = page.getByRole('combobox').first()
    await expect(watchlistSelect).toContainText(listRenamed)

    await createWatchlist(page, listB)
    await expect(watchlistSelect).toContainText(listB)

    await watchlistSelect.selectOption({ label: listB })
    await expect(page.getByText('No symbols in this watchlist yet.')).toBeVisible()

    page.once('dialog', (dialog) => dialog.accept())
    await page.getByLabel('Delete watchlist').click()

    await expect(watchlistSelect).not.toContainText(listB)
    await expect(watchlistSelect).toContainText(listRenamed)
  })

  test('symbol add/sort/remove plus invalid symbol error handling', async ({ page, request }) => {
    await page.addInitScript(() => {
      class DisabledWebSocket {
        constructor() {
          this.readyState = 3
        }
        close() {}
      }
      window.WebSocket = DisabledWebSocket
    })

    await bootstrapLoginViaUI(page, request)
    await createWatchlist(page, `E2E Symbols ${Date.now()}`)

    await addSymbol(page, 'AAPL')
    await addSymbol(page, 'MSFT')
    await addSymbol(page, 'TSLA')

    const symbolCells = page.locator('tbody tr td:first-child')
    await expect(symbolCells).toHaveCount(3)

    await page.getByRole('columnheader', { name: /Price/ }).click()
    await expect(symbolCells.first()).toContainText('MSFT')

    await page.getByRole('columnheader', { name: /Price/ }).click()
    await expect(symbolCells.first()).toContainText('AAPL')

    const tslaRow = page.locator('tbody tr', { hasText: 'TSLA' })
    await tslaRow.hover()
    await tslaRow.getByLabel('Remove TSLA').click()
    await expect(page.locator('tbody tr td:first-child', { hasText: 'TSLA' })).toHaveCount(0)

    await page.route('**/api/watchlists/*/symbols', async (route) => {
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON()
        if (body?.symbol === 'BAD!') {
          await route.fulfill({ status: 400, contentType: 'text/plain', body: 'Invalid symbol format' })
          return
        }
      }
      await route.continue()
    })

    page.once('dialog', (dialog) => dialog.accept())
    await addSymbol(page, 'BAD!')
    await expect(page.getByText('Invalid symbol format')).toBeVisible()
  })

  test('stock detail symbol sync delay + interval and chart controls', async ({ page, request }) => {
    await bootstrapLoginViaUI(page, request)
    await createWatchlist(page, `E2E Chart ${Date.now()}`)

    await addSymbol(page, 'AAPL')
    await addSymbol(page, 'MSFT')

    const msftCell = page.locator('tbody tr td:first-child', { hasText: 'MSFT' })
    await msftCell.click()

    await expect(page.getByRole('heading', { name: 'MSFT' })).toBeVisible({ timeout: 5000 })

    const aaplCell = page.locator('tbody tr td:first-child', { hasText: 'AAPL' })
    await expect(aaplCell).toBeVisible()
    await expect(msftCell).toBeVisible()

    await aaplCell.click()
    await expect(page.getByRole('heading', { name: 'AAPL' })).toBeVisible({ timeout: 5000 })

    await page.getByRole('button', { name: '1D' }).click()
    await expect(page.getByTestId('interval-state')).toHaveText('1D:1')

    await page.getByRole('button', { name: '1W' }).click()
    await expect(page.getByTestId('interval-state')).toHaveText('1W:5')

    await page.getByRole('button', { name: '1Y' }).click()
    await expect(page.getByTestId('interval-state')).toHaveText('1Y:60')

    await expect(page.getByRole('button', { name: 'Candlestick 🔒' })).toBeDisabled()
    await page.getByRole('button', { name: 'Mountain' }).click()
    await expect(page.getByRole('heading', { name: 'AAPL' })).toBeVisible()
  })

  test('pricing checkout CTA shows failure then success states', async ({ page, request }) => {
    await bootstrapLoginViaUI(page, request)

    let checkoutCalls = 0
    await page.route('**/api/billing/checkout-session', async (route) => {
      checkoutCalls += 1

      if (checkoutCalls === 1) {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ message: 'mock failure' })
        })
        return
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          username: 'pw_user',
          requestedPlan: 'PRO',
          status: 'NOT_CONFIGURED',
          sessionId: 'mock_session_123',
          checkoutUrl: '/pricing?checkout=mock&plan=PRO'
        })
      })
    })

    await page.goto('/pricing')
    await expect(page.getByRole('heading', { name: 'Pricing' })).toBeVisible()

    await page.getByRole('button', { name: 'Choose Pro' }).click()
    await expect(page.getByText('Request failed (500)')).toBeVisible()

    await page.getByRole('button', { name: 'Choose Pro' }).click()
    await expect(page.getByText('Checkout placeholder ready:')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Open checkout URL' })).toBeVisible()
  })

  test('websocket quote stream connects and receives quote frames', async ({ page, request }) => {
    await bootstrapLoginViaUI(page, request)
    await createWatchlist(page, `E2E WS ${Date.now()}`)

    const webSocketPromise = page.waitForEvent('websocket', {
      predicate: (ws) => ws.url().includes('/ws/quotes')
    })

    await addSymbol(page, 'MSFT')
    const ws = await webSocketPromise

    const frameReceived = new Promise((resolve, reject) => {
      const timeoutId = setTimeout(() => reject(new Error('No quote frame received from websocket')), 15000)
      ws.on('framereceived', (event) => {
        try {
          const payload = JSON.parse(event.payload)
          if (payload?.type === 'quotes' && Array.isArray(payload?.quotes) && payload.quotes.length > 0) {
            clearTimeout(timeoutId)
            resolve(payload)
          }
        } catch {
          // ignore non-json frames
        }
      })
    })

    const payload = await frameReceived
    expect(payload.type).toBe('quotes')
    expect(payload.quotes.length).toBeGreaterThan(0)
  })
})
