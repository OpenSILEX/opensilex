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

test('TP variable crud working version', async ({ page }) => {
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

  // Clicking on Scientific Information link
    const scientificInfoLink = page.getByTestId('component.menu.experimentalDesign.label');
    await expect(scientificInfoLink).toBeVisible({ timeout: 20000 });
    await scientificInfoLink.click();

  // Clicking on Variables menu
    const variablesMenu = page.getByTestId('component.menu.variables');
    await expect(variablesMenu).toBeVisible({ timeout: 20000 });
    await variablesMenu.click();



  ///////////////////////////
  // ADD a variable 
  /////////////////////////// 

    const elementName = 'VariableTestAuto';

    // Open creation modal
      const addVariableButton = page.getByRole('button' , { name: 'Add variable'});
      await expect(addVariableButton).toBeVisible({ timeout: 20000});
      await addVariableButton.click();
      const variableCreationModal = page.getByTestId('variableCreateModalForm');

    // Create with an entity
      const entitySelector = variableCreationModal.getByTestId('entitySelector');
      await entitySelector.click();
      const entityOption = entitySelector.getByText('Plant', {exact: true});
      await expect(entityOption).toBeVisible({ timeout: 20000 });
      await entityOption.click();

    // and a characteristic
      const characteristicSelector = variableCreationModal.getByTestId('characteristicSelector');
      await characteristicSelector.click();
      const characteristicOption = characteristicSelector.getByText('Height');
      await expect(characteristicOption).toBeVisible({ timeout: 20000 });
      await characteristicOption.click();

    // and a method
      const methodSelector = variableCreationModal.getByTestId('methodSelector');
      await methodSelector.click();
      const methodOption = methodSelector.getByText('Measurement');
      await expect(methodOption).toBeVisible({ timeout: 20000 });
      await methodOption.click();

    // and an unit
      const unitSelector = variableCreationModal.getByTestId('unitSelector');
      await unitSelector.click();
      const unitOption = unitSelector.getByText('Percentage');
      await expect(unitOption).toBeVisible({ timeout: 20000 });
      await unitOption.click();
    
    
    // and a name
      const nameSelector = variableCreationModal.getByLabel('Name', {exact: true});
      await nameSelector.click();
      await nameSelector.fill(elementName);

    // and a data type
      const dataTypeSelector = variableCreationModal.getByTestId('variableDataTypeSelector');
      await dataTypeSelector.click();
      const dataTypeOption = dataTypeSelector.getByText('Decimal number');
      await expect(dataTypeOption).toBeVisible({ timeout: 20000 });
      await dataTypeOption.click();

    // validate the creation
      await variableCreationModal.getByRole('button', { name: 'OK' }).click();


  ///////////////////////////
  // UPDATE / READ a variable
  /////////////////////////// 

    // Expect to be on variable details page
      await page.waitForTimeout(5000);
      const variableDetailPage = page.getByTestId('variableDescriptionCard');
      await expect(variableDetailPage).toBeVisible({ timeout: 20000});

    // Open update modal
      const updateVariableButton = page.getByRole('button' , { name: 'Edit variable'});
      await expect(updateVariableButton).toBeVisible({ timeout: 20000});
      await updateVariableButton.click();

    // Update variable name
      await page.waitForTimeout(3000);
      await page.getByLabel('Name', { exact: true}).fill(elementName + "Edited");
      await page.getByLabel('Name', { exact: true}).press('Enter');
      await page.getByRole('button', { name: 'OK'}).click;

    // Expect to be back on variable details page
      await page.waitForTimeout(5000);
      await expect(variableDetailPage).toBeVisible({ timeout: 20000});

    // Go back to variables list
      const returnToVariablesButton = page.locator('.back-button');
      await expect(returnToVariablesButton).toBeVisible({ timeout: 20000 });
      await returnToVariablesButton.click();

    // Search the updated variable by name filter
      await page.waitForTimeout(5000);
      await page.getByTitle('Search filters').first().click();
      await page.getByPlaceholder('Enter variable name').click();
      await page.getByPlaceholder('Enter variable name').fill(elementName + "Edited");
      await page.getByPlaceholder('Enter variable name').press('Enter');

    // Expect to found the variable on results
      const searchedVariableName = page.locator(`text=${elementName + "Edited"}`);
      await expect(searchedVariableName).toBeVisible({ timeout: 20000});


  ///////////////////////////
  // DELETE a selected device
  /////////////////////////// 

    // Target the delete button on this specific line and click him
      const selectedDeviceRow = page.locator(`text=${elementName}`).locator('..').locator('..').locator('..').locator('..').locator('..').locator('..');
      const deleteButton = selectedDeviceRow.locator('button[title="Delete"]');
      await expect(deleteButton).toBeVisible( { timeout: 20000});
      await deleteButton.click();

    // Target the modal button and confirm deletion
      const deleteConfirmationModal = page.locator('.modal-dialog-centered .modal-content');
      await expect(deleteConfirmationModal).toBeVisible({ timeout: 20000 });
      const deleteConfirmationButton = deleteConfirmationModal.getByRole('button', { name: 'delete' });
      await deleteConfirmationButton.click();

});