import { expect, test } from '@playwright/test';

async function loginAsCustomer(page: import('@playwright/test').Page) {
  await page.goto('/login');
  await page.getByLabel('Email').fill('terry@test.com');
  await page.locator('input[type="password"]').fill('password');
  await page.getByRole('button', { name: /^login$/i }).click();
  await expect(page.getByRole('heading', { name: /welcome back, terry/i })).toBeVisible();
}

test.describe.serial('customer experience', () => {
  test('customer can sign in and browse the catalog', async ({ page }) => {
    await loginAsCustomer(page);

    await page.getByRole('link', { name: 'Top 20', exact: true }).click();
    await expect(page).toHaveURL(/\/movies\?top20=true/);
    await expect(page.getByRole('heading', { name: /top 20 movies/i })).toBeVisible();
    await expect(page.getByLabel('Movie results')).toBeVisible();
    await expect(page.getByLabel(/view .+/i).first()).toBeVisible();
  });

  test('customer can search, add a movie to the cart, and check out', async ({ page }) => {
    await loginAsCustomer(page);

    await page.goto('/movies?q=Spider');
    await expect(page.getByRole('heading', { name: /search results for "spider"/i })).toBeVisible();
    await page.getByRole('button', { name: /^add$/i }).first().click();
    await expect(page.getByText(/movie added to cart/i)).toBeVisible();

    await page.goto('/cart');
    await expect(page.getByRole('heading', { name: /shopping cart/i })).toBeVisible();
    await expect(page.getByRole('table')).toContainText(/spider/i);

    await page.getByRole('button', { name: /proceed to payment/i }).click();
    await expect(page.getByRole('heading', { name: /complete order/i })).toBeVisible();
    await page.getByLabel(/first name/i).fill('Terry');
    await page.getByLabel(/last name/i).fill('Kim');
    await page.getByLabel(/credit card number/i).fill('9999000011112222');
    await page.getByLabel(/expiration date/i).fill('2030-12-31');
    await page.getByRole('button', { name: /place order/i }).click();

    await expect(page).toHaveURL(/\/confirmation/);
    await expect(page.getByRole('heading', { name: /order placed/i })).toBeVisible();
    await expect(page.getByRole('table')).toContainText(/spider/i);
  });
});
