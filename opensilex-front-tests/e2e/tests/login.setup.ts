/*
 * *****************************************************************************
 *                         global-setup.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 14/06/2024 10:16
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import test, { chromium, expect } from '@playwright/test';
import { LOGIN_STATE } from '../playwright.config';

test('Login setup', async (_config) => {
    const browser = await chromium.launch();
    const page = await browser.newPage();
    await page.goto(process.env.APP_URL);

    await page.waitForTimeout(10000);

    // Connect as admin
    await page.getByTestId('default-login-component-email-input').click();
    await page.getByTestId('default-login-component-email-input').fill('admin@opensilex.org');
    await page.getByTestId('default-login-component-password-input').click();
    await page.getByTestId('default-login-component-password-input').fill('admin');
    await page.getByTestId('default-login-component-connection-button').click();

    await expect(page.getByTestId('dashboard-page-header')).toBeVisible({ timeout: 60000 });

    await page.context().storageState({ path: LOGIN_STATE });
    await browser.close();
})
