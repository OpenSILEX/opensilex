/*
 * *****************************************************************************
 *                         test-1.spec.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 11/06/2024 13:11
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto('app/');
});

test('menu', async ({ page }) => {
  await page.locator('#menu-container').getByRole('button').click();
  await page.getByRole('link', { name: '  Data' }).click();
  await page.getByRole('link', { name: ' Tabular Data' }).click();
});
