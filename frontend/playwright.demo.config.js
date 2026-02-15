const base = require('./playwright.config')

module.exports = {
  ...base,
  fullyParallel: false,
  workers: 1,
  use: {
    ...base.use,
    headless: false,
    launchOptions: {
      slowMo: 2000
    }
  }
}
