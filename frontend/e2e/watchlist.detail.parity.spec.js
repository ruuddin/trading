const { test, expect } = require('@playwright/test')

async function login(page, request) {
  const username = `parity_${Date.now()}_${Math.floor(Math.random() * 10000)}`
  const password = 'Pass123!'

  const registerResponse = await request.post('http://localhost:8080/api/auth/register', {
    data: { username, password }
  })
  expect([200, 409]).toContain(registerResponse.status())

  const loginResponse = await request.post('http://localhost:8080/api/auth/login', {
    data: { username, password }
  })
  expect(loginResponse.status()).toBe(200)
  const loginPayload = await loginResponse.json()

  await page.goto('/')
  await page.evaluate(({ token, user }) => {
    window.localStorage.setItem('token', token)
    window.localStorage.setItem('username', user)
  }, { token: loginPayload.token, user: username })
  await page.goto('/')
  await expect(page.getByText(`Signed in as ${username}`)).toBeVisible()
}

function parseMoney(text) {
  if (!text) return NaN
  const normalized = String(text).replace(/[^0-9.\-]/g, '')
  return Number(normalized)
}

test('watchlist and detail price stay in sync for selected symbol', async ({ page, request }) => {
  await login(page, request)

  const createNewWatchlistButton = page.getByLabel('Create new watchlist')
  if (await createNewWatchlistButton.count()) {
    await createNewWatchlistButton.click()
  } else {
    await page.getByRole('button', { name: 'Create Your First Watchlist' }).click()
  }

  await page.getByPlaceholder('Watchlist name').fill(`Parity ${Date.now()}`)
  await page.getByRole('button', { name: 'Create', exact: true }).click()

  await page.getByLabel('Add symbol').click()
  await page.getByPlaceholder('Stock symbol (e.g., AAPL, MSFT)').fill('MSFT')
  await page.getByRole('button', { name: 'Add', exact: true }).click()

  await expect(page.locator('tbody tr td:first-child', { hasText: 'MSFT' })).toBeVisible()

  const row = page.locator('tbody tr', { hasText: 'MSFT' })
  await row.click()

  await expect(page.getByRole('heading', { name: 'MSFT' })).toBeVisible({ timeout: 7000 })

  const wlPriceCell = row.locator('td').nth(1)
  await expect(wlPriceCell).toContainText('$', { timeout: 7000 })

  const detailPriceLocator = page.locator('span').filter({ hasText: /^\$\d+\.\d{2}$/ }).first()
  await expect(detailPriceLocator).toBeVisible({ timeout: 7000 })
  const detailPriceText = await detailPriceLocator.innerText()
  const wlPriceText = await wlPriceCell.innerText()

  const wlPrice = parseMoney(wlPriceText)
  const detailPrice = parseMoney(detailPriceText)

  expect(Number.isFinite(wlPrice)).toBeTruthy()
  expect(Number.isFinite(detailPrice)).toBeTruthy()

  const diff = Math.abs(wlPrice - detailPrice)
  expect(diff).toBeLessThanOrEqual(0.01)
})
