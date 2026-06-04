import { expect, test } from '@playwright/test';

test.describe('employee dashboard', () => {
  test('employee can sign in, inspect metadata, and add a genre', async ({ page }) => {
    await page.goto('/employee/login');
    await page.getByLabel('Email').fill('admin@fabflix.com');
    await page.locator('input[type="password"]').fill('admin');
    await page.getByRole('button', { name: /^login$/i }).click();

    await expect(page.getByRole('heading', { name: /database metadata/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'movies', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'customers', exact: true })).toBeVisible();

    await page.getByRole('link', { name: /add genre/i }).click();
    await expect(page.getByRole('heading', { name: /add new genre/i })).toBeVisible();

    const genreName = `Playwright-${Date.now()}`;
    await page.getByLabel(/genre name/i).fill(genreName);
    await page.getByRole('button', { name: /save/i }).click();
    await expect(page.getByText(new RegExp(`Successfully added genre: ${genreName}`))).toBeVisible();
  });
});
