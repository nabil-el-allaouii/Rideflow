import { expect, Page } from '@playwright/test';

export function uniqueEmail(prefix: string): string {
  const stamp = `${Date.now()}-${Math.floor(Math.random() * 10_000)}`;
  return `${prefix}.${stamp}@rideflow.local`;
}

export async function registerCustomer(page: Page, email: string): Promise<void> {
  await page.goto('/auth/register');
  await page.getByRole('textbox', { name: 'Full Name' }).fill('E2E Customer');
  await page.getByRole('textbox', { name: 'Email' }).fill(email);
  await page.getByRole('textbox', { name: 'Phone Number' }).fill('+212600123456');
  await page.locator('input[aria-label="Password"]').fill('Password123!');
  await page.getByRole('button', { name: 'Create Account' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

export async function login(page: Page, email: string, password: string, expectedPath: RegExp): Promise<void> {
  await page.goto('/auth/login');
  await page.getByRole('textbox', { name: 'Email' }).fill(email);
  await page.locator('input[aria-label="Password"]').fill(password);
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page).toHaveURL(expectedPath);
}
