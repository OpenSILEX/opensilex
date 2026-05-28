# Storybook Setup for OpenSilex Front‑end

## 1. Introduction
This document describes how to install, configure, and use **Storybook** for developing, visualising, and testing Vue 3 components in the OpenSilex front‑end. Storybook is integrated via the `@storybook/vue3-vite` framework and includes several addons (Chromatic, Vitest, A11y, Docs, Onboarding) together with a custom i18n configuration.

## 2. Prerequisites
| Tool | Minimum version |
|------|-----------------|
| Node.js | 22.x |
| npm | 10.x |
| Vue | 3.5.x |
| Vite | 6.3.x |
| Git | required for Chromatic builds |

Ensure the above are installed before proceeding.

## 3. Install dependencies
```bash
npm ci    # exact lock‑file installation
# or npm install if no lockfile is present
```
Using `npm ci` guarantees the exact versions defined in the lock file.

## 4. npm scripts for Storybook
| Script | Description |
|--------|-------------|
| `npm run storybook` | Starts the Storybook dev server on **port 6006** (`storybook dev -p 6006`). |
| `npm run build-storybook` | Builds a static version of Storybook (`storybook build`). Output is placed in `storybook-static`.

Run them with:
```bash
npm run storybook          # http://localhost:6006
npm run build-storybook    # generates ./storybook-static
```

## 5. `.storybook` directory structure
- **`.storybook/main.ts`** – Main configuration (stories location, addons, framework, Vite customisation).
- **`.storybook/preview.ts`** – Global decorator that initialises Vue‑I18n for all stories.

No other files are required.

## 6. Detailed `main.ts` configuration
```ts
import type { StorybookConfig } from '@storybook/vue3-vite';
import type { InlineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const config: StorybookConfig = {
  // 1️⃣ Stories pattern
  stories: ['../opensilex-front/front/src/stories/*.ts'],

  // 2️⃣ Addons
  addons: [
    '@chromatic-com/storybook',   // visual testing / Chromatic CI
    '@storybook/addon-vitest',   // Vitest integration
    '@storybook/addon-a11y',     // accessibility checks
    '@storybook/addon-docs',     // MDX documentation
    '@storybook/addon-onboarding' // interactive onboarding guide
  ],

  // 3️⃣ Framework
  framework: '@storybook/vue3-vite',

  // 4️⃣ Custom Vite configuration
  async viteFinal(config: InlineConfig): Promise<InlineConfig> {
    // alias for vue‑i18n CJS build
    (config.resolve!.alias as Record<string, string>)['vue-i18n'] =
      path.resolve(__dirname, '../node_modules/vue-i18n/dist/vue-i18n.cjs.js');

    // ensure Vue plugin is present
    if (!config.plugins?.some(p => (p as any).name === 'vite:vue')) {
      config.plugins = [...(config.plugins ?? []), vue()];
    }

    // add Vue I18n plugin for <i18n> blocks
    if (!config.plugins?.some(p => (p as any).name === 'vue-i18n')) {
      config.plugins = [
        ...(config.plugins ?? []),
        VueI18nPlugin({ defaultSFCLang: 'json' })
      ];
    }

    return config;
  },
};

export default config;
```
Key points:
- **Stories** are loaded from `opensilex-front/front/src/stories/`.
- **Addons** provide visual testing, unit‑test integration, accessibility checks, documentation generation, and onboarding.
- **Vite tweaks** resolve `vue-i18n` to the CommonJS bundle and guarantee the Vue and i18n plugins are active.

## 7. `preview.ts` – Global decorator
```ts
import type { Preview } from '@storybook/vue3';
import { setup } from '@storybook/vue3';
import { createI18n } from 'vue-i18n';
import en from '../opensilex-front/front/src/lang/message-en.json';
import fr from '../opensilex-front/front/src/lang/message-fr.json';

const i18n = createI18n({
  legacy: false,           // required for Composition API / useI18n()
  locale: 'en',
  fallbackLocale: 'en',
  messages: { en, fr },
});

setup(app => {
  app.use(i18n);
});

export const preview: Preview = {
  parameters: {
    actions: {},
    controls: { expanded: true },
  },
};
```
The decorator injects an i18n instance with English and French translations, enabling multilingual component stories.

## 8️⃣ Using Storybook day‑to‑day
- **Start locally**: `npm run storybook` → `http://localhost:6006`.
- **Accessibility panel**: open the *A11Y* tab to see WCAG violations.
- **Vitest tests**: click the *Tests* tab (provided by `@storybook/addon-vitest`) to run unit tests directly.
- **Publish to Chromatic** (CI):
  ```bash
  npx chromatic --project-token <YOUR_TOKEN>
  ```
  Ensure `CHROMATIC_PROJECT_TOKEN` is set in the environment.
- **Static build**: `npm run build-storybook` creates `./storybook-static`, which can be deployed on any static host (GitLab Pages, Netlify, Vercel, …).

## 9️⃣ Best Practices
1. Keep addons up‑to‑date; the repo pins them to `^10.4.1`.
2. Add new locales to `opensilex-front/front/src/lang/` and import them in `preview.ts`.
3. Store stories alongside components or in a dedicated `stories/` folder, following the pattern `ComponentName.stories.ts`.
4. In CI pipelines, run the steps:
   - `npm ci`
   - `npm test` (Vitest)
   - `npx chromatic` for visual regression.
5. If builds become slow, consider caching Vite's `.vite` directory in CI.

## 🔟 Further Resources
| Resource | Link |
|----------|------|
| Storybook Vue 3 docs | https://storybook.js.org/docs/vue3/get-started/introduction |
| Vite integration guide | https://storybook.js.org/docs/vue3/configure/vite |
| Vitest addon | https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon |
| Chromatic visual testing | https://www.chromatic.com/docs |
| Vue‑I18n Vite plugin | https://vue-i18n.intlify.dev/guide/essentials/plugin.html |

---
*This file was generated automatically to document the Storybook setup for the OpenSilex front‑end.*