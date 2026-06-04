import { defineConfig, devices } from '@playwright/test';

const isCi = Boolean(process.env.CI);

export default defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  expect: {
    timeout: 10_000
  },
  fullyParallel: false,
  reporter: isCi ? [['html', { open: 'never' }], ['github']] : 'list',
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: [
    {
      command: 'sh -lc "cd .. && set -a; [ -f .env ] && . ./.env; set +a; mvn spring-boot:run"',
      url: 'http://localhost:8080/api/health',
      timeout: 120_000,
      reuseExistingServer: !isCi
    },
    {
      command: 'npm run dev -- --host 0.0.0.0 --port 5173',
      url: 'http://localhost:5173/login',
      timeout: 60_000,
      reuseExistingServer: !isCi
    }
  ]
});
