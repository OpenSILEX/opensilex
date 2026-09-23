import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { resolve } from 'path'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'

export default defineConfig(({ mode }) => ({
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      'opensilex-core': resolve(__dirname, '../../opensilex-core/front/src'),
      'opensilex-security': resolve(__dirname, '../../opensilex-security/front/src')
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
