import { test } from '@playwright/test';

test('admin-connection', async ({ page }) => {
  await page.goto('http://localhost:8080/app/');
  await page.getByTestId('default-login-component-email-input').click();
  await page.getByTestId('default-login-component-email-input').fill('admin@opensilex.org');
  await page.getByTestId('default-login-component-password-input').click();
  await page.getByTestId('default-login-component-password-input').fill('admin');
  await page.getByTestId('default-login-component-connection-button').click();
  page.getByTestId('dashboard-page-header');
});