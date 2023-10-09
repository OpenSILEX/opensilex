import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('/app/');
});

test('menu', async ({ page }) => {
  await page.locator('#menu-container').getByRole('button').click();
  await page.getByRole('link', { name: '  Data' }).click();
  await page.getByRole('link', { name: ' Tabular Data' }).click();
});
