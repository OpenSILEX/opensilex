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

test('CRUD of a device', async ({ page }) => {
  await expect(page).toHaveURL('http://localhost:8080/app');

  // Clicking on language options dropdown
    const dropdownButton = page.locator('#__BVID__32__BV_toggle_');
    await dropdownButton.click();

  // Clicking on first option (english)
    const dropdownMenu = page.locator('.dropdown-menu.show');
    await expect(dropdownMenu).toBeVisible({ timeout: 20000 });
    const firstLanguageOption = dropdownMenu.locator('li').first();
    await firstLanguageOption.click();

  // Clicking on global menu button 
    await page.locator('#menu-container').getByRole('button').click();

  // Clicking on Scientific Organization link
    const scientificOrgLink = page.getByTestId('component.menu.scientific-organisation');
    await expect(scientificOrgLink).toBeVisible({ timeout: 20000 });
    await scientificOrgLink.click();

  // Clicking on Devices menu
    const devicesMenu = page.getByTestId('component.menu.devices');
    await expect(devicesMenu).toBeVisible({ timeout: 20000 });
    await devicesMenu.click();


  ///////////////////////////
  // ADD a device
  /////////////////////////// 

  const elementName = 'TestAuto';

    // Open creation modal
      const addDeviceButton = page.getByRole('button' , { name: 'Add device' });
      await expect(addDeviceButton).toBeVisible({ timeout: 20000 });
      await addDeviceButton.click();

    // With a type
      const deviceCreationModal = page.getByTestId('deviceModalForm')

      // must be careful to select the one on modal because typeSelector exist on global search filters too, same for name
      const typeSelector = deviceCreationModal.getByTestId('typeSelector');
      await typeSelector.click();
      const typeOption = typeSelector.getByText('RGB camera');
      await expect(typeOption).toBeVisible({ timeout: 20000 });
      await typeOption.click();
    // With a name
      await deviceCreationModal.getByLabel('Name').click();
      await deviceCreationModal.getByLabel('Name').fill(elementName);
    // Validate the creation
      await deviceCreationModal.getByRole('button', { name: 'OK' }).click();


    // Automatic redirection on device detail after creation
    // Expect to be on device details page
      await page.waitForTimeout(5000);
      const deviceDetailPage = page.getByTestId('deviceDescriptionCard');
      await expect(deviceDetailPage).toBeVisible({ timeout: 20000});

    // Go back to devices list
      const returnToDevicesButton = page.locator('.back-button');
      await expect(returnToDevicesButton).toBeVisible({ timeout: 20000 });
      await returnToDevicesButton.click();

    // Search the device by name filter
      await page.waitForTimeout(5000);
      await page.getByTitle('Search filters').first().click();
      await page.getByPlaceholder('Enter name').click();
      await page.getByPlaceholder('Enter name').fill(elementName);
      await page.getByPlaceholder('Enter name').press('Enter');

    // Expect to found the device on list or results
      const searchedDeviceName = page.locator(`text=${elementName}`);
      await expect(searchedDeviceName).toBeVisible({ timeout: 20000});


  ///////////////////////////
  // UPDATE a selected device
  /////////////////////////// 

    // Target the line containing the device to update / delet
    // .locator('..') allows you to go back through the DOM to the parents, here the <tr> tag
      const selectedDeviceRow = page.locator(`text=${elementName}`).locator('..').locator('..').locator('..').locator('..').locator('..').locator('..');

    // Open update modal
      const updateButton = selectedDeviceRow.locator('button[title="Update Device"]');
      await expect(updateButton).toBeVisible( { timeout: 20000});
      await updateButton.click();

    // Update device description
      await page.waitForTimeout(5000);
      await page.getByLabel('Description').fill(' camera ajoutée dans le cadre des tests automatiques du front');

    // Validate update
      await page.getByRole('button', { name: 'OK' }).click();


  ///////////////////////////
  // READ a selected device
  ///////////////////////////

    // Target the URI link and click her. Locator ".." target direct parent
      const elementLink = page.locator(`text=${elementName}`).locator('..');
      await expect(elementLink).toBeVisible({ timeout: 20000});
      await elementLink.click();

    // Expect to be on device details page
      await expect(deviceDetailPage).toBeVisible({ timeout: 20000});

    // Expect description field to be filled
      const deviceDetailDescriptionField = page.getByText('camera ajoutée dans le cadre des tests automatiques du front');
      await expect (deviceDetailDescriptionField).toBeVisible({ timeout: 20000});

    // Go back to devices list
      await expect(returnToDevicesButton).toBeVisible({ timeout: 20000 });
      await returnToDevicesButton.click();

  ///////////////////////////
  // DELETE a selected device
  /////////////////////////// 

    // Target the delete button on this specific line and click him
      const deleteButton = selectedDeviceRow.locator('button[title="Delete Device"]');
      await expect(deleteButton).toBeVisible( { timeout: 20000});
      await deleteButton.click();

    // Target the modal button and confirm deletion
      const deleteConfirmationModal = page.locator('.modal-dialog-centered .modal-content');
      await expect(deleteConfirmationModal).toBeVisible({ timeout: 20000 });
      const deleteConfirmationButton = deleteConfirmationModal.getByRole('button', { name: 'delete' });
      await deleteConfirmationButton.click();

    // Open search pannel
      await page.locator('.searchMenuIcon > .icon').click();

    // Clean filters
      await page.getByRole('button', { name: 'Reset' }).click();

    // Search the device by her name
      await page.getByPlaceholder('Enter name').click();
      await page.getByPlaceholder('Enter name').fill(elementName);
      await page.getByPlaceholder('Enter name').press('Enter');

    // Expect to didn't found him
    await expect(searchedDeviceName).toBeHidden({ timeout: 20000});

});