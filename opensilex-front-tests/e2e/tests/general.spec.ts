/*
 * *****************************************************************************
 *                         general.spec.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/06/2024 15:42
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import {test, expect, chromium} from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.goto(process.env.APP_URL);
});

test('menu', async ({ page }) => {
  await page.locator('#menu-container').getByRole('button').click();
  await page.getByTestId("component.menu.data.label").click();
  await page.getByTestId("component.menu.data.tabularData").click();
});
