import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { resolve } from 'path'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'
import { heyApiPlugin } from '@hey-api/vite-plugin'

export default defineConfig(({ mode }) => ({
  resolve: {
    alias: {
      '~bootstrap': resolve(__dirname, 'node_modules/bootstrap'),
      '@': resolve(__dirname, 'src'),
      'opensilex-security': resolve(__dirname, '../../opensilex-security/front/src/index.ts'),
      'opensilex-security/*': resolve(__dirname, '../../opensilex-security/front/src/*'),
      'opensilex-core': resolve(__dirname, '../../opensilex-core/front/src/index.ts'),
      'opensilex-core/*': resolve(__dirname, '../../opensilex-core/front/src/*'),
      'opensilex-front': resolve(__dirname, 'src/index.ts'),
      'opensilex-front/*': resolve(__dirname, 'src/*')

    },
  },

  plugins: [
    heyApiPlugin({
      config: {
        input: resolve(__dirname, 'src/lib/openapi.json'),
        output: resolve(__dirname, 'src/lib/generated'),
        plugins: [
          '@hey-api/client-fetch',
          '@hey-api/typescript',
          '@hey-api/sdk'
        ]
      }
    }),
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

  test: {
    environment: 'happy-dom',
    globals: true,
  },
}))
