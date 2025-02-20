/*
 * *****************************************************************************
 *                         playwright.config.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/06/2024 13:09
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import {defineConfig, PlaywrightTestConfig} from '@playwright/test';

export const LOGIN_STATE = 'login.state.json';

let DEFAULT_BASE_URL = "http://localhost:8080"

console.log("===============================================ENV===============================================")
console.log(process.env)
console.log("=================================================================================================")

console.log(process.env.BASE_ADDRESS)
console.log("=================================================================================================")
if (!process.env.BASE_ADDRESS || process.env.BASE_ADDRESS == 'false') {
    process.env.BASE_URL = DEFAULT_BASE_URL
} else {
    process.env.BASE_URL = process.env.BASE_ADDRESS
}

process.env.APP_URL = process.env.BASE_URL + "/app"
console.log(process.env.BASE_URL)
console.log("=================================================================================================")

let COMMAND = './opensilex_test_instance/start_opensilex.sh'

if (process.env.DOCKERISED_BASES && process.env.DOCKERISED_BASES != 'false') {
    COMMAND += ' -d'
}

if (process.env.MAVEN_BUILD && process.env.MAVEN_BUILD != 'false') {
    COMMAND += ' -b'
}


let webServer = process.env.START_OPENSILEX && process.env.START_OPENSILEX != 'false' 
    ? {
        command: COMMAND,
        url: process.env.APP_URL,
        reuseExistingServer: !process.env.CI,
        timeout: 1200000, // 20 minutes
        stdout: "pipe",
        stderr: "pipe"
    } 
    : undefined;

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
        trace: 'retain-on-failure'
    },
    /* Configure projects for major browsers */
    projects: [
        {
            name: 'auth-tests',
            testMatch: '**/*.auth.spec.ts',
            use: {
                storageState: undefined
            }
        },
        {
            name: 'login-setup',
            testMatch: '**/login.setup.ts'
        },
        {
            name: 'app-tests',
            testMatch: '**/*.app.spec.ts',
            dependencies: ['login-setup'],
            use: {
                storageState: LOGIN_STATE
            }
        }
    ],

    /* Run your local dev server before starting the tests */
    webServer: webServer
}

console.log(config)
export default defineConfig(config);