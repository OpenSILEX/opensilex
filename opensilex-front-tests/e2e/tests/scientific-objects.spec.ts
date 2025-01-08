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

test('CRUD of a scientific object', async ({ page }) => {
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

  // Clicking on Scientific object menu
    const scientificObjectsMenu = page.getByTestId('component.menu.scientificObjects');
    await expect(scientificObjectsMenu).toBeVisible({ timeout: 20000 });
    await scientificObjectsMenu.click();


  ///////////////////////////
  // ADD a scientific object
  /////////////////////////// 

    const elementName = 'TestAuto';

    // Open creation modal
      const addScientificObjectButton = page.getByRole('button' , { name: 'Add scientific object' });
      await expect(addScientificObjectButton).toBeVisible({ timeout: 20000 });
      await addScientificObjectButton.click();
      const scientificObjectCreationModal = page.getByTestId('scientificObjectModalForm')

    // Create with a name
      // must be careful to select the one on modal because multiple nameSelector exist on this page, same for typeSelector
      const nameSelector = page.getByTestId('OntologyObjectForm-nameSelector').nth(1);
      const nameSelectorInput = nameSelector.getByPlaceholder('Enter object name');
      await nameSelectorInput.fill(elementName)

    // And with a type
      const typeSelector = scientificObjectCreationModal.getByTestId('typeSelector');
      await typeSelector.click();
      const typeOption = typeSelector.getByText('Pot');
      await expect(typeOption).toBeVisible({ timeout: 20000 });
      await typeOption.click();

    // Validate the creation
      await scientificObjectCreationModal.getByRole('button', { name: 'OK' }).click();

    // Search the scientific object 
      await page.waitForTimeout(5000);
      await page.getByTitle('Search filters').first().click();

    // By her name
      await page.getByPlaceholder('Enter name').click();
      await page.getByPlaceholder('Enter name').fill(elementName);

    // And by her type
      const scientificTypeSelector = page.getByTestId('scientificObjectsView-typeSelector');
      await expect(scientificTypeSelector).toBeVisible({ timeout: 20000 });
      await scientificTypeSelector.locator('.vue-treeselect__input-container').click();
      // options available only after the click on the input
      await page.locator('.vue-treeselect__option >> text=Pot').click();

      const searchOSButton = page.getByRole('button' , { name: 'Search' });
      await expect(searchOSButton).toBeVisible({ timeout: 20000 });
      await searchOSButton.click();

    // Expect to found the OS on list or results
      // This following step will not work if more than one item exists with that name
      const searchedOSName = page.locator(`text=${elementName}`);
      await expect(searchedOSName).toBeVisible({ timeout: 20000});


  ///////////////////////////
  //// READ a selected OS
  ///////////////////////////

    // Target the URI link and click her. Locator ".." target direct parent
      const elementLink = page.locator(`text=${elementName}`).locator('..');
      await expect(elementLink).toBeVisible({ timeout: 20000});
      await elementLink.click();

    // Expect to be on OS details page
      const scientificObjectDetailPage = page.getByTestId('scientificObjectDetailProperties');
      await expect(scientificObjectDetailPage).toBeVisible({ timeout: 20000});


  ///////////////////////////
  // UPDATE a selected OS
  /////////////////////////// 

    // Target update OS button
      const objectDetailUpdateButton = page.getByTestId('scientificObjectDetail-updateButton');
      await expect(objectDetailUpdateButton).toBeVisible({ timeout: 20000});
      await objectDetailUpdateButton.click();

    // Replace OS name by an other
      const editedElementName = 'TestAutoEdited';
      await page.waitForTimeout(5000);
      const updateNameSelector = page.getByTestId('OntologyObjectForm-nameSelector').nth(0);
      const updateNameSelectorInput = updateNameSelector.getByPlaceholder("Enter object name");
      await updateNameSelectorInput.fill(editedElementName);

    // Validate update
      await page.getByRole('button', { name: 'OK' }).click();

    // Expect name field on the card description to be changed
      const objectDetailCard = page.locator('.card-body');
      const objectDetailNameField = objectDetailCard.getByText('TestAutoEdited');
      await expect (objectDetailNameField).toBeVisible({ timeout: 20000});


  /////////////////////////////
  //// DELETE a selected OS
  ///////////////////////////// 

    // Target delete OS button
      const objectDetailDeleteButton = page.getByTestId('scientificObjectDetail-deleteButton');
      await expect(objectDetailDeleteButton).toBeVisible({ timeout: 20000});
      await objectDetailDeleteButton.click();

    // Target the modal and confirm deletion
      const deleteConfirmationModal = page.locator('.modal-dialog-centered .modal-content');
      await expect(deleteConfirmationModal).toBeVisible({ timeout: 20000 });
      const deleteConfirmationButton = deleteConfirmationModal.getByRole('button', { name: 'delete' });
      await deleteConfirmationButton.click();

    
    // Redirected on OS list, search again the OS
      await page.waitForTimeout(5000);
      await page.getByTitle('Search filters').first().click();

    // By her name
      await page.getByPlaceholder('Enter name').click();
      await page.getByPlaceholder('Enter name').fill(editedElementName);

    // And by her type
      await expect(scientificTypeSelector).toBeVisible({ timeout: 20000 });
      await scientificTypeSelector.locator('.vue-treeselect__input-container').click();
      await page.locator('.vue-treeselect__option >> text=Pot').click();

      await expect(searchOSButton).toBeVisible({ timeout: 20000 });
      await searchOSButton.click();

    // Expect to didn't found an OS with this name
      const searchedObjectName = page.locator(`text=${editedElementName}`);
      await expect(searchedObjectName).toBeHidden({ timeout: 20000});
});