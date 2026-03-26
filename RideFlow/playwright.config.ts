import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';

const frontendRoot = __dirname;
const backendRoot = path.resolve(__dirname, '../RideFlow-Backend');

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:4300',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: [
    {
      command:
        '.\\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=e2e" "-Dspring-boot.run.useTestClasspath=true"',
      cwd: backendRoot,
      url: 'http://127.0.0.1:18080/api/scooters/available',
      reuseExistingServer: false,
      stdout: 'pipe',
      stderr: 'pipe',
      timeout: 180_000
    },
    {
      command: 'npm start -- --host 127.0.0.1 --port 4300 --configuration e2e',
      cwd: frontendRoot,
      url: 'http://127.0.0.1:4300',
      reuseExistingServer: false,
      stdout: 'pipe',
      stderr: 'pipe',
      timeout: 120_000
    }
  ]
});
