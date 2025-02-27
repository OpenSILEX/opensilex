import { test, expect } from '@playwright/test';

test.beforeEach(async ({ page }, testInfo) => {
  console.log(`Running ${testInfo.title}`);
  try {
    await page.goto(process.env.APP_URL, { timeout: 20000 });
  } catch (error) {
    console.log('First page.goto timed out, trying again');
    await page.goto(process.env.APP_URL, { timeout: 20000 });
  }
});

test('max os crud test', async ({ page }) => {
  await expect(page).toHaveURL('http://localhost:8081/app/');

  // Click menu button because screen always starts too small
  await page.locator('#menu-container').getByRole('button').click();

  //Click scientific objects page in menu
  const scientificInformation = page.getByTestId('component.menu.experimentalDesign.label');
  await expect(scientificInformation).toBeVisible({ timeout: 20000 });
  await scientificInformation.click();
  const scientificObjectsPage = page.getByTestId('component.menu.scientificObjects');
  await expect(scientificObjectsPage).toBeVisible({ timeout: 20000 });
  await scientificObjectsPage.click();

  //Add a scientific object
  const addScientificObjectButton = page.getByRole('button', {name: 'Add scientific object'});
  await expect(addScientificObjectButton).toBeVisible({timeout: 20000});
  await addScientificObjectButton.click();
  //Get modal, await for first thing to fill, then fill fields
  //name
  const sciObjCreateModal = page.getByTestId('scientificObjectModalForm');
  const nameField = page.getByTestId('OntologyObjectForm-nameSelector').nth(1);
  await expect(nameField).toBeVisible({timeout: 20000});
  await nameField.click();
  const nameSelectorInput = nameField.getByPlaceholder('Enter object name');
  await nameSelectorInput.fill('testyOs');
  //type
  const typeSelector = sciObjCreateModal.getByTestId('typeSelector');
  await typeSelector.click();
  await typeSelector.getByText('plot', { exact: true }).click();
  await sciObjCreateModal.getByRole('button', { name: 'OK' }).click();

  //Leave and re-enter scientific objects page because of the annoying refresh bug
  await page.getByTestId('component.menu.variables').click();
  await page.getByTestId('component.menu.scientificObjects').click();

  //Rename our scientific object
  await page.locator('[id="__BVID__1578__row_sixtine-vigne\\:id\\/scientific-object\\/so-testyos"]').getByRole('button', { name: 'Edit scientific object' }).click();
  await page.getByRole('textbox', { name: 'Name *' }).click();
  await page.getByRole('textbox', { name: 'Name *' }).fill('testyOsRenamed');
  await page.getByRole('button', { name: 'OK' }).click();
  await page.goto('http://localhost:8081/app/scientific-objects/details/sixtine-vigne%3Aid%2Fscientific-object%2Fso-testyos');

  //Go back to scientific object list, locate and delete our OS
  await page.getByTestId('component.menu.scientificObjects').click();
  await page.locator('[id="__BVID__2498__row_sixtine-vigne\\:id\\/scientific-object\\/so-testyos"]').getByRole('button', { name: 'Delete scientific object' }).click();
  await page.getByRole('button', { name: 'delete', exact: true }).click();
});