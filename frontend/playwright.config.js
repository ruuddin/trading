const { defineConfig } = require('@playwright/test')

module.exports = defineConfig({
  testDir: './e2e',
  timeout: 120000,
  fullyParallel: true,
  retries: 0,
  workers: process.env.CI ? 1 : 2,
  webServer: {
    command: 'npm run dev -- --host localhost --port 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 120000
  },
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  }
})
