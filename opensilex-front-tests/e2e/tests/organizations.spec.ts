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

test('CRUD of an organization facility', async ({ page }) => {
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

  // Clicking on Organizations menu
    const organizationMenu = page.getByTestId('component.menu.organization');
    await expect(organizationMenu).toBeVisible({ timeout: 20000 });
    await organizationMenu.click();

  // Clicking on a choosen organization
    const organization = page.getByText('PHENOPSIS').first();
    await expect(organization).toBeVisible({ timeout: 20000 });
    await organization.click();


  ///////////////////////////
  // ADD a facility to the selected organization
  /////////////////////////// 

    // open creation modal
      const addFacilityButton = page.getByRole('button' , { name: 'Add facility' });
      await expect(addFacilityButton).toBeVisible({ timeout: 20000 });
      await addFacilityButton.click();
    // With a type
      const typeSelector = page.getByTestId('typeSelector');
      await typeSelector.click();
      const typeOption = typeSelector.getByText('Field');
      await expect(typeOption).toBeVisible({ timeout: 20000 });
      await typeOption.click();
    // With a name
      await page.getByPlaceholder('Enter object name').click();
      await page.getByPlaceholder('Enter object name').fill('TestAuto'); // remplacer par un hash random ?
    // Validate the creation
      await page.getByRole('button', { name: 'OK' }).click();


  ///////////////////////////
  // READ a selected facility details by her name
  ///////////////////////////

    await page.waitForTimeout(5000);
    const elementName = 'TestAuto';
    
    // Target the URI link and click her
      const elementLink = page.locator(`text=${elementName}`).locator('..');
      await expect(elementLink).toBeVisible({ timeout: 20000});
      await elementLink.click();
    
    // Expect to be on facility details page
      const facilityDetailPage = page.locator('.facilityDescription');
      await expect(facilityDetailPage).toBeVisible({ timeout: 20000});
    
    // Go back to the organizations list
      const returnToOrganizationsButton = page.locator('.back-button');
      await expect(returnToOrganizationsButton).toBeVisible({ timeout: 20000 });
      await returnToOrganizationsButton.click();
    
    // Select again the same organization
      await expect(organization).toBeVisible({ timeout: 20000 });
      await organization.click();
    

  // Target the line containing the facility to update / delet
  // .locator('..') allows you to go back through the DOM to the parents, here the <tr> tag
    const row = page.locator(`text=${elementName}`).locator('..').locator('..').locator('..').locator('..');


  ///////////////////////////
  // UPDATE a selected facility details by her name
  ///////////////////////////

  // Open update modal
    const updateButton = row.locator('button[title="Update facility"]');
    await expect(updateButton).toBeVisible( { timeout: 20000});
    await updateButton.click();

  // Update facility adress
    await page.waitForTimeout(5000); // necessary because cant use toBeVisible on checkbox pseudo-elements (::before ::after)
    const updateFacilityAdressCheckbox = page.getByTestId('facility-adress-checkbox');
    await updateFacilityAdressCheckbox.dispatchEvent("click");
    await page.getByPlaceholder('City').click();
    await page.getByPlaceholder('City').fill('Montpellier'); 

  // Validate update
    await page.getByRole('button', { name: 'OK' }).click();


  ///////////////////////////
  // DELETE a selected facility by her name
  ///////////////////////////
  
  // Target the delete button on this specific line and click him
    const deleteButton = row.locator('button[title="Delete facility"]');
    await expect(deleteButton).toBeVisible( { timeout: 20000});
    await deleteButton.click();

  // Target the modal button and confirm deletion
    const deleteConfirmationModal = page.locator('.modal-dialog-centered .modal-content');
    await expect(deleteConfirmationModal).toBeVisible({ timeout: 20000 });
    const deleteConfirmationButton = deleteConfirmationModal.getByRole('button', { name: 'delete' });
    await deleteConfirmationButton.click();
});