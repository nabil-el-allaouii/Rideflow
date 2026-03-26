import { expect, test } from '@playwright/test';
import { registerCustomer, uniqueEmail } from './helpers/auth';

test.describe('Customer rental flow', () => {
  test('customer can configure payment, cancel a reservation, complete a ride, and open a receipt', async ({
    page
  }) => {
    const primaryMenu = page.getByRole('navigation', { name: 'Primary menu' });
    const email = uniqueEmail('customer');

    await registerCustomer(page, email);

    await primaryMenu.getByRole('button', { name: 'Browse Scooters' }).click();
    await expect(page).toHaveURL(/\/scooters$/);
    await expect(page.getByRole('button', { name: /Set payment method in profile/i }).first()).toBeDisabled();

    await primaryMenu.getByRole('button', { name: 'My Profile' }).click();
    await expect(page).toHaveURL(/\/profile$/);
    await page.locator('select[formcontrolname="paymentMethod"]').selectOption('WALLET');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    await expect(page.getByText('Saving...')).toHaveCount(0);

    await primaryMenu.getByRole('button', { name: 'Browse Scooters' }).click();
    await expect(page).toHaveURL(/\/scooters$/);

    const unlockButton = page.getByRole('button', { name: /Unlock & Ride/i }).first();
    await expect(unlockButton).toBeEnabled();

    await unlockButton.click();
    await expect(page.getByText('Current Ride Status')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Start Ride' })).toBeVisible();

    await page.getByRole('button', { name: 'Cancel Ride' }).click();
    await expect(page.getByRole('button', { name: 'Start Ride' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /Unlock & Ride/i }).first()).toBeEnabled();

    await page.getByRole('button', { name: /Unlock & Ride/i }).first().click();
    await page.getByRole('button', { name: 'Start Ride' }).click();
    await expect(page.getByRole('button', { name: 'End Ride' })).toBeVisible();

    await page.waitForTimeout(1200);
    await page.getByRole('button', { name: 'End Ride' }).click();
    await expect(page.getByRole('button', { name: 'End Ride' })).toHaveCount(0);

    await primaryMenu.getByRole('button', { name: 'Rental History' }).click();
    await expect(page).toHaveURL(/\/history$/);

    const receiptButton = page.locator('button.receipt-button:not([disabled])').first();
    await expect(receiptButton).toBeVisible();
    await receiptButton.click();

    await expect(page).toHaveURL(/\/history\/\d+\/receipt$/);
    await expect(page.getByRole('heading', { name: /Receipt/i })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Download PDF' })).toBeVisible();
    await expect(page.getByText('Pricing Breakdown')).toBeVisible();
  });
});
