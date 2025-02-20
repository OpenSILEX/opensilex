import { test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto(process.env.APP_URL);
})

test('Login as guest should fail', async ({ page }) => {
  await page.getByTestId('guest-connexion-button').click()
  //attendu: afficher le message d'erreur
});

test('Login with wrong credentials should fail', async ({ page }) => {
  await page.getByTestId('default-login-component-email-input').click();
  await page.getByTestId('default-login-component-email-input').fill('admin@opensilex');
  await page.getByTestId('default-login-component-password-input').click();
  await page.getByTestId('default-login-component-password-input').fill('adm');
  await page.getByTestId('default-login-component-connection-button').click();// messages d'erreur non reportés
  //attendu: afficher les message d'erreur
})

test('Login with correct credentials', async ({ page }) => {
  await page.getByTestId('default-login-component-email-input').click();
  await page.getByTestId('default-login-component-email-input').fill('admin@opensilex.org');
  await page.getByTestId('default-login-component-password-input').click();
  await page.getByTestId('default-login-component-password-input').fill('admin');
  await page.getByTestId('default-login-component-connection-button').click();
  //attendu : vérifier qu'on est bien loggé
})