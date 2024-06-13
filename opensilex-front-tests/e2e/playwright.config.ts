/*
 * *****************************************************************************
 *                         playwright.config.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 13/06/2024 15:40
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

import { defineConfig, devices } from '@playwright/test';
import * as path from 'path';

process.env.BASE_URL = "http://localhost:8080"
process.env.APP_URL = process.env.BASE_URL + "/app"

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
// require('dotenv').config();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
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
  webServer: {
    command: '../../opensilex-release/target/opensilex/opensilex.sh server start --CONFIG_FILE=../../../opensilex-dev-tools/src/main/resources/config/opensilex.yml --port=8080',
    url: process.env.APP_URL,
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
    stdout: "pipe",
    stderr: "pipe"
  },
});
