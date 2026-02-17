import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { resolve } from 'path'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'

export default defineConfig(({ mode }) => ({
  resolve: {
    alias: {
      '~bootstrap': resolve(__dirname, 'node_modules/bootstrap'),
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
    outDir: 'dist',
    rollupOptions: {
      external: ['vue'],
      output: {
        globals: { vue: 'Vue' },
      },
    },
  },

  server: {
    port: 8080,
  },

  base: '/app/',
}))
