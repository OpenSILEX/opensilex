/*
 * *****************************************************************************
 *                         playwright.config.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 21/06/2024 11:43
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import {defineConfig, PlaywrightTestConfig} from '@playwright/test';

let DEFAULT_BASE_URL = "http://localhost:8080"

if (!process.env.BASE_URL) {
    process.env.BASE_URL = DEFAULT_BASE_URL
}

process.env.APP_URL = process.env.BASE_URL + "/app"

let COMMAND = './opensilex_test_instance/start_opensilex.sh'

if (process.env.DOCKERISED_BASES) {
    COMMAND += ' -d'
}

let webServer = undefined;

if (process.env.START_OPENSILEX) {
    webServer = {
        command: COMMAND,
        url: process.env.APP_URL,
        reuseExistingServer: !process.env.CI,
        timeout: 1200000, // 20 minutes
        stdout: "pipe",
        stderr: "pipe"
    }
}

/**
 * See https://playwright.dev/docs/test-configuration.
 */
let config : PlaywrightTestConfig = {
    testDir: './tests',
    /* Run tests in files in parallel */
    fullyParallel: true,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: !!process.env.CI,
    /* Retry on CI only */
    retries: process.env.CI ? 2 : 0,
    /* Opt out of parallel tests on CI. */
    workers: process.env.CI ? 1 : undefined,
    /* Reporter to use. See https://playwright.dev/docs/test-reporters */
    reporter: 'html',
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: 'retain-on-failure',
        storageState: 'state.json'
    },

    globalSetup: require.resolve('./global-setup'),

    /* Configure projects for major browsers */
    projects: [
        {
            name: 'chromium'
        },

        {
            name: 'firefox'
        },

        {
            name: 'webkit'
        },

        /* Test against mobile viewports. */
        // {
        //   name: 'Mobile Chrome',
        //   use: { ...devices['Pixel 5'] },
        // },
        // {
        //   name: 'Mobile Safari',
        //   use: { ...devices['iPhone 12'] },
        // },

        /* Test against branded browsers. */
        // {
        //   name: 'Microsoft Edge',
        //   use: { ...devices['Desktop Edge'], channel: 'msedge' },
        // },
        // {
        //   name: 'Google Chrome',
        //   use: { ...devices['Desktop Chrome'], channel: 'chrome' },
        // },
    ],

    /* Run your local dev server before starting the tests */
    webServer: webServer
}

console.log(config)
export default defineConfig(config);