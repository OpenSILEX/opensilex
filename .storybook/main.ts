// .storybook/main.ts
import type { StorybookConfig } from '@storybook/vue3-vite';
import type { InlineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const config: StorybookConfig = {
  stories: [
    '../opensilex-front/front/src/stories/*.ts'
  ],
  addons: [
    '@chromatic-com/storybook',
    '@storybook/addon-vitest',
    '@storybook/addon-a11y',
    '@storybook/addon-docs',
    '@storybook/addon-onboarding'
  ],
  framework: '@storybook/vue3-vite',

  async viteFinal(config: InlineConfig): Promise<InlineConfig> {
    if (!config.plugins) config.plugins = [];
    if (!config.resolve) config.resolve = {};
    if (!config.resolve.alias) config.resolve.alias = {};

    // Fix vue-i18n resolution
    (config.resolve.alias as Record<string, string>)['vue-i18n'] = path.resolve(
      __dirname,
      '../node_modules/vue-i18n/dist/vue-i18n.cjs.js'
    );

    // Ensure Vue plugin is present for .vue handling
    if (!config.plugins.some((p) => p && (p as { name?: string }).name === 'vite:vue')) {
      config.plugins.push(vue());
    }

    // Add Vue I18n plugin so <i18n> blocks are compiled
    if (!config.plugins.some((p) => p && (p as { name?: string }).name === 'vue-i18n')) {
      config.plugins.push(
        VueI18nPlugin({
          defaultSFCLang: 'json',
        })
      );
    }

    return config;
  },
};

export default config;