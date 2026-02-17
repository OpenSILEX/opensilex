import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import vuemain.ts: 388[Vue warn]: Invalid prop name: "$opensilex" is a reserved property.
  at<RouterView>
at<App>
prepare.js: 1[Vue warn]: Invalid watch source:
{ fullPath: '/', path: '/', query: {… }, hash: '', name: 'default', … }
 A watch source can only be a getter / effect function, a ref, a reactive object, or an array of these types.
  vue - router.js ? v = 67454260 : 1792 Uncaught(in promise) TypeError: Cannot read properties of undefined(reading 'matched')
    at formatRouteLocation(vue - router.js ? v = 67454260 : 1792: 28)
    at vue - router.js ? v = 67454260 : 1835: 18
    at prepare.js: 1: 61094
    at Array.forEach(<anonymous>)
    at prepare.js: 1: 61083
    at Et.callHookWith(prepare.js: 1: 35674)
    at prepare.js: 1: 61064
    at prepare.js: 1: 64944
    at Array.map(<anonymous>)
    at prepare.js: 1: 64937
 from 'vite-plugin-vue-devtools';
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';

export default defineConfig({
  resolve: {
    alias: {
      '~bootstrap': resolve(__dirname, 'node_modules/bootstrap')
    }
  },

  // optimizeDeps: {
  //   include: ['vue', 'vue-router']
  // },

  plugins: [
    vue(),
    vueDevTools(),
    VueI18nPlugin({
      defaultSFCLang: "yaml"
    })
  ], // Utilisez le plugin Vue pour Vite
  // css: {
  //   preprocessorOptions: {
  //     scss: {
  //       // additionalData: `@import "/theme.scss";`
  //     }
  //   }
  // },
  assetsInclude: ['**/*.md'],
  build: {
    outDir: 'dist', // Dossier de sortie
    // lib: {
    //   entry: resolve(__dirname, 'src/lib/index.ts'), // Point d'entrée
    //   name: 'opensilex-front', // Nom de la bibliothèque
    //   fileName: (format) => `opensilex-front.${format}.js`, // Nom du fichier de sortie
    //   formats: ['es', 'umd'], // Formats de sortie (ES Module et UMD)
    // },
    rollupOptions: {
      // Externaliser les dépendances (si nécessaire)
      external: ['vue'],
      output: {
        globals: {
          vue: 'Vue',
          // 'vue-router': 'VueRouter',
        },
      },
    },
  },
  // for dev
  server: {
    port: 8080,
  },
  base: '/app/', // Chemin de base pour les ressources
});