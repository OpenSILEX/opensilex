# opensilex-front-tests

This repo contains tools for automated testing of the frontend of OpenSILEX.


## Playwright help for testers

### Setup

Isntall vscode and start it. In the extensions menu on the left (four squares symbol) search for "Playwright Test for VSCode" and install the extension.
__TODO : Check if install needed__
You can now go to the "Testing" menu on the left (Flask icon). The existing tests will appear in the "TEST EXPLORER" sub-menu. At the bottom left you can find the "PLAYWRIGHT" sub-menu.

### Recording new test template

To record a new test template pres the "Record new" option in the "PLAYWRIGHT" sub-menu. This should open a browser. You can now Perform the different actions to be recorded as a test. These actions will be saved as a ".spec.ts" file and will appear in the "TEST EXPLORER" sub-menu.
In the file "Explorer" menu on the left you can now find the created file in the "tests" directory. Send this test template to a developper to be checked and integrated to the tests. To help this process you should accompany it with a description of the different steps that were taken.

## Help for devs

__[Playwright docs](https://playwright.dev/docs)__
The test templates you will receive will need refinement especially for the locators. Different types of locators are available (<https://playwright.dev/docs/other-locators>) but the best practice is to use `data-testid`s.
