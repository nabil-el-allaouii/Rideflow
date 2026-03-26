import { expect, test } from '@playwright/test';
import { login } from './helpers/auth';

test.describe('Admin operations', () => {
  test('admin can review the dashboard, update pricing, create a scooter, and inspect monitoring pages', async ({
    page
  }) => {
    const scooterCode = `PW-E2E-${Date.now()}`;

    await login(page, 'admin.seed@rideflow.local', 'Password123!', /\/admin\/dashboard$/);

    await expect(page.getByRole('heading', { name: 'Admin Dashboard' })).toBeVisible();
    await expect(page.getByText('Weekly Revenue')).toBeVisible();

    await page.getByRole('button', { name: 'Pricing' }).click();
    await expect(page).toHaveURL(/\/admin\/pricing$/);
    const unlockFeeInput = page.locator('input[formcontrolname="unlockFee"]');
    const ratePerMinuteInput = page.locator('input[formcontrolname="ratePerMinute"]');
    const batteryConsumptionInput = page.locator('input[formcontrolname="batteryConsumptionRate"]');

    await expect(page.locator('article.metric-card', { hasText: 'Current Unlock Fee' })).toContainText(
      '1.00'
    );
    await expect(unlockFeeInput).toHaveValue('1');
    await expect(ratePerMinuteInput).toHaveValue('0.2');
    await expect(batteryConsumptionInput).toHaveValue('0.5');

    await unlockFeeInput.fill('1.35');
    await ratePerMinuteInput.fill('0.25');
    await batteryConsumptionInput.fill('0.60');
    await expect(unlockFeeInput).toHaveValue('1.35');
    await expect(ratePerMinuteInput).toHaveValue('0.25');
    await expect(batteryConsumptionInput).toHaveValue('0.60');
    await page.getByRole('button', { name: 'Save Pricing' }).click();
    await expect(page.getByText('Pricing updated successfully.')).toBeVisible();
    await expect(page.locator('article.metric-card', { hasText: 'Current Unlock Fee' })).toContainText(
      '1.35'
    );

    await page.getByRole('button', { name: 'Scooter Fleet' }).click();
    await expect(page).toHaveURL(/\/admin\/fleet$/);
    await page.getByRole('button', { name: 'Add Scooter' }).click();
    await page.locator('input[formcontrolname="publicCode"]').fill(scooterCode);
    await page.locator('input[formcontrolname="model"]').fill('Playwright Test Scooter');
    await page.locator('input[formcontrolname="batteryPercentage"]').fill('88');
    await page.locator('input[formcontrolname="latitude"]').fill('33.589886');
    await page.locator('input[formcontrolname="longitude"]').fill('-7.603869');
    await page.locator('input[formcontrolname="address"]').fill('Playwright Test Hub');
    await page.locator('input[formcontrolname="kilometersTraveled"]').fill('12.4');
    await page.getByRole('button', { name: 'Create Scooter' }).click();
    await expect(page.getByText(scooterCode)).toBeVisible();

    await page.getByRole('button', { name: 'User Management' }).click();
    await expect(page).toHaveURL(/\/admin\/users$/);
    await expect(page.getByRole('heading', { name: 'User Management' })).toBeVisible();
    await expect(page.getByText('Visible Users')).toBeVisible();

    await page.getByRole('button', { name: 'Payments' }).click();
    await expect(page).toHaveURL(/\/admin\/payments$/);
    await expect(page.getByRole('heading', { name: 'Payments' })).toBeVisible();
    await expect(page.locator('table')).toContainText('TX-UNLOCK-0001');

    await page.getByRole('button', { name: 'Rental Monitor' }).click();
    await expect(page).toHaveURL(/\/admin\/rentals$/);
    await expect(page.getByRole('heading', { name: 'Rental Monitor' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible();
  });
});
