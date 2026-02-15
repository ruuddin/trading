const { defineConfig } = require('@playwright/test')

module.exports = defineConfig({
  testDir: './e2e',
  timeout: 120000,
  fullyParallel: false,
  retries: 0,
  workers: 1,
  webServer: {
    command: 'npm run dev -- --host localhost --port 3000',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
    timeout: 120000
  },
  use: {
    baseURL: 'http://localhost:3000',
    headless: false,
    launchOptions: {
      slowMo: 2000
    },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  }
})
