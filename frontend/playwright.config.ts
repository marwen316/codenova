import {defineConfig} from '@playwright/test';
const target = process.env['E2E_BASE_URL'];
export default defineConfig({
  testDir: './e2e',
  webServer: target ? undefined : {
    command: 'npm start -- --port 4200',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 60000,
  },
  use: {
    baseURL: target || 'http://localhost:4200',
    headless: true,
    launchOptions: {
      executablePath: process.env['CHROMIUM_PATH'] || '/usr/bin/chromium',
      args: ['--no-sandbox'],
    },
  },
  workers: 1,
  reporter: 'list',
});
