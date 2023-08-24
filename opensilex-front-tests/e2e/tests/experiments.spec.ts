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

test('experiment visualisation', async ({ page }) => {
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


  // Clicking on Experiments menu
    const experimentsMenu = page.getByTestId('component.menu.experiments');
    await expect(experimentsMenu).toBeVisible({ timeout: 20000 });
    await experimentsMenu.click();


  // Going to page 2 
  // ! Be careful to number of elements displayed for a page on your local (10, 20, 50 elements)
  // Tests are running on private navigation and use the option of 20 elements per page
  // If your local choice is different you can be on the wrong page
    const page2Link = page.getByLabel('Go to page 2');
    await expect(page2Link).toBeVisible({ timeout: 30000 });
    await page2Link.click();


  // Click on link of the choosen experiment 
  // ! Be careful when you record a test, the button to copy experiment URI is taken too
  // Give you something like : { name: 'ZA17 ' } and didn't work
    const experimentLink = page.getByRole('link', { name: 'ZA17' });
    await expect(experimentLink).toBeVisible({ timeout: 20000 });
    await experimentLink.click();


  // Clicking on Visualization Tab link
    const visualizationLink = page.getByRole('link', { name: 'Visualization' });
    await expect(visualizationLink).toBeVisible({ timeout: 20000 });
    await visualizationLink.click();


  // Clicking on OS Selector
    const osSelector = page.locator('.container-full > div > div').first();
    await expect(osSelector).toBeVisible({ timeout: 20000 });
    await osSelector.click();


  // Clicking on checkbox of a choosen OS
    await page.waitForTimeout(5000); // necessary because cant use toBeVisible on checkbox pseudo-elements (::before ::after)
    await page.locator('#scientificObjectSelector #checkboxTestID1').dispatchEvent("click");


  // Validate OS selection
    const validateOSSelectionButton = page.getByRole('button', { name: 'Validate the selection' });
    await expect(validateOSSelectionButton).toBeVisible({ timeout: 20000 });
    await validateOSSelectionButton.click();


  // Clicking on VAR Selector
    const varSelector = page.locator('#variableSelector');
    await expect(varSelector).toBeVisible({ timeout: 20000 });
    await varSelector.click();


  // Clicking on checkbox of a choosen VAR
    await page.waitForTimeout(5000); // necessary because cant use toBeVisible on checkbox pseudo-elements (::before ::after)
    await page.locator('#variableSelector #checkboxTestID0').dispatchEvent("click");
  

  // Clicking on validation button for VAR selection
    const validateVarSelectionButton = page.getByRole('button', { name: 'Validate the selection' });
    await expect(validateVarSelectionButton).toBeVisible({ timeout: 20000 });
    await validateVarSelectionButton.click();
 

  // Clicking on visualisation button
    // const visualizeButton = page.getByRole('button', { name: 'Visualize' });
    // await expect(visualizeButton).toBeVisible({ timeout: 20000 });
    await page.waitForTimeout(5000);
    const visualizeButton = page.getByText('Visualize').locator('..');
    await visualizeButton.click();


  // Graphic container must be visible
    await page.waitForTimeout(5000);
    const graph = page.locator('.graphContainer'); 
    await expect(graph).toBeVisible({ timeout: 20000 });
});
