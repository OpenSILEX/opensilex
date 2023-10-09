# opensilex-front-tests

This repo contains tools for automated testing of the frontend of OpenSILEX.

## Integration in OpenSILEX dev workflow

The steps of the end-to-end testing is as follows :

* scenario conception = what is usually done by a user when using the new feature
* test template recording = recording of the scenario on the solution that was developed
* template correction = correcting the test template to get a finalised, working, stable test.
* run test

The development of the end-to-end tests will be integrated in the usual development workflow in multiple places :

* The scenario conception will be inserted before the development step. This will help the developer to understand the functionality.
* The test template recording will be done just before the template correction
* The template correction will be done just before the test step
* The tests will be run in multiple places (some tests will be run on developer branch deployment, all on the develop branch and on release creation)

## (optional) Setup vscode

Install vscode and start it. In the extensions menu on the left (four squares symbol) search for "Playwright Test for VSCode" and install the extension.
__TODO : Check if install needed__
You can now go to the "Testing" menu on the left (Flask icon). The existing tests will appear in the "TEST EXPLORER" sub-menu. At the bottom left you can find the "PLAYWRIGHT" sub-menu.

## (mandatory) Install Playwright

To install playwright run the following command in opensilex-front-tests/e2e :

```sh
npm install 
```

## Playwright help for testers

### Scenario creation

The scenario file describes every step that is taken for a usage scenario.
It has a descriptive name that will be reused for the test name.
It assumes that the user is already connected as an admin.
See "scenario-template.md".

### Recording new test template

To record a new test template pres the "Record new" option in the "PLAYWRIGHT" sub-menu. This should open a browser. You can now Perform the different actions to be recorded as a test. These actions will be saved as a ".spec.ts" file in the "tests" folder and will appear in the "TEST EXPLORER" sub-menu.
This will be called "test template" from now on.
__NOTE__ : This template will contain __ALL THE ACTIONS YOU PERFORMED DURING THE RECORDING__. If you have made a small mistake during recording you can simply inform the developer about it later.
If you've made several mistakes it is recommended to make a new recording and delete the failed one. To do so, you can simply delete the generated template from the "tests" folder (in the same folder as this help file).
If you are satisfied with the result, send this test template file to a developer to be checked and integrated to the tests. To help this process you should accompany it with a description of the mistakes made if relevant and details that aren't in the scenario file.

## Help for developers

### Running the tests locally

If you're using vscode the interface is self-explanatory.

Otherwise, you have different options :

* UI mode :

```sh
npx playwright test --ui
```

* Command line run all tests :

```sh
npx playwright test
```

* Command line run single test file :

```sh
npx playwright test <file-name>
```

See [playwright docs](https://playwright.dev/docs/running-tests) for more details and options.

### Gitlab-ci

### Template refining

__[Playwright docs](https://playwright.dev/docs)__
The test templates you will receive will need refinement especially for the locators. Different types of locators are available (<https://playwright.dev/docs/other-locators>) but the best practice is to use `data-testid`s.
If this locator doesn't already exist on the components used in the test template you should add them manually.
Using the adminLoggedPage fixture from opensilex-test-fixtures.ts will give you a base state where the user is connected as admin.

## Possible improvements

* Caching the rdf4j and mongodb images used by gitlab-ci would speed up tests
* Test files and scenarios can be consolidated according to OpenSILEX elements (ex: all tests for "Experiments" in a single file)
* Integrate to maven tests
* Add credentials in a config file
