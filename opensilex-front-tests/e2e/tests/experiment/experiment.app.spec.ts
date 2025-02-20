import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  //derouler le menu
  await page.locator('#menu-container').getByRole('button').click();
  //click scientific organization 
  await page.getByRole('link', { name: '  Scientific Organization' }).click();
  //ouverture Experiments  
  await page.getByRole('link', { name: ' Experiments' }).click();
  //Add experiment
  await page.getByRole('button', { name: 'Add experiment' }).click();
  //Name
  await page.getByPlaceholder('ZA17').click();
  await page.getByPlaceholder('ZA17').fill('TestAuto');
  //start Date
  await page.getByLabel('Friday, February 7,').click();
  await page.getByLabel('Start date').click();
  //Objectives
  await page.getByPlaceholder('Genomic prediction of maize yield ', { exact: true }).click();
  await page.getByPlaceholder('Genomic prediction of maize yield ', { exact: true }).fill('Test Auto');
  await page.getByPlaceholder('Genomic prediction of maize yield across European environmental scenarios').click();
  //Next
  await page.getByRole('button', { name: 'Next' }).click();
  //Scientific qupervisors (plusieurs)
  await page.locator('[id="__BVID__536"]').getByRole('group').locator('div').filter({ hasText: 'Search persons...' }).nth(3).click();
  await page.getByText('admin admin <admin@opensilex.').click();
  await page.getByText('Etienne Belin <etienne.belin@').click();
  await page.locator('[id="__BVID__336___BV_modal_body_"]').click();
  await page.locator('div:nth-child(9) > .vue-treeselect__option > .vue-treeselect__label-container > .vue-treeselect__checkbox-container').click();
  await page.getByText('Etienne Belin <etienne.belin@univ-angers.fr>and 1 moreSearch persons...').click();
  await page.getByText('and 1 more').click();
  await page.getByLabel('2').locator('div').filter({ hasText: 'Scientific supervisors' }).nth(2).click();
  //technical supervisor (1)
  await page.locator('[id="__BVID__556"]').getByRole('group').locator('div').filter({ hasText: 'Search persons...' }).nth(3).click();
  await page.getByText('David Rousseau <david.').click();
  await page.getByLabel('2').locator('div').filter({ hasText: 'Technical supervisors' }).nth(2).click();
  //projects
  await page.locator('[id="__BVID__576"]').getByRole('group').locator('div').filter({ hasText: 'Search projects' }).nth(3).click();
  await page.getByLabel('2').getByText('EPPN2020').click();
  //
  await page.getByRole('link', { name: '2', exact: true }).click();
  //Organizations

  await page.locator('[id="__BVID__593"]').getByRole('group').locator('div').filter({ hasText: 'Search organizations' }).nth(3).click();

  await page.locator('div:nth-child(3) > div > .vue-treeselect__label-container > .vue-treeselect__checkbox-container > .vue-treeselect__checkbox > .vue-treeselect__minus-mark').first().click();
  //Facility
  await page.locator('div:nth-child(11) > .vue-treeselect__option > .vue-treeselect__label-container > .vue-treeselect__checkbox-container > .vue-treeselect__checkbox').click();
  await page.locator('[id="__BVID__626"] > div > .input-group > .vue-treeselect > .vue-treeselect__control > .vue-treeselect__value-container > .vue-treeselect__multi-value > .vue-treeselect__input-container').click();
  await page.locator('[id="__BVID__610"]').getByRole('group').locator('div').filter({ hasText: 'Search and select a facility' }).nth(3).click();
  //Group

  await page.locator('div').filter({ hasText: /^Search groups\.\.\.$/ }).nth(2).click();

  //Public/private (changer 3 fois)

  await page.getByText('Select this option to make').click();
  await page.getByText('Select this option to make').click();
  await page.getByText('Select this option to make').click();

  //Save et retour page expé ajoutée

  await page.getByRole('button', { name: 'Save' }).click();

  //update

  await page.getByRole('button', { name: 'Update experiment' }).click();
  //add description
  await page.getByPlaceholder('Genomic prediction of maize yield across European environmental scenarios').click();
  await page.getByPlaceholder('Genomic prediction of maize yield across European environmental scenarios').fill('test auto');
  await page.getByRole('button', { name: 'Next' }).click();
  //add new project
  await page.getByText('EPPN2020Search projects').click();
  await page.getByText('FFG Moutarde').click();
  await page.getByRole('button', { name: 'Save' }).click();


});