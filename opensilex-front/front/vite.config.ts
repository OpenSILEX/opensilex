import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { resolve } from 'path'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'

export default defineConfig(({ mode }) => ({
  resolve: {
    alias: {
      '@': resolve(import.meta.dirname, 'src'),
      'opensilex-core': resolve(import.meta.dirname, '../../opensilex-core/front/src'),
      'opensilex-security': resolve(import.meta.dirname, '../../opensilex-security/front/src')
    },
  },

  plugins: [
    vue(),
    ...(mode === 'development' ? [vueDevTools()] : []),
    VueI18nPlugin({
      defaultSFCLang: 'yaml',
    }),
  ],

  assetsInclude: ['**/*.md'],

  build: {
    outDir: 'dist'
  },

  server: {
    port: 8080,
  },

  base: '/app/',
}))
