/*
 * *****************************************************************************
 *                         global.setup.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 11/06/2024 17:16
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import { test as setup, expect } from '@playwright/test';
import { STORAGE_STATE } from '../playwright.config';

setup('do login as admin', async ({ page }) => {
    await page.goto('/app', { timeout: 10000 });

    // Connect as admin
    await page.getByTestId('default-login-component-email-input').click();
    await page.getByTestId('default-login-component-email-input').fill('admin@opensilex.org');
    await page.getByTestId('default-login-component-password-input').click();
    await page.getByTestId('default-login-component-password-input').fill('admin');
    await page.getByTestId('default-login-component-connection-button').click();

    await expect(page.getByTestId('dashboard-page-header')).toBeVisible({ timeout: 10000 });

    await page.context().storageState({ path: STORAGE_STATE });
});