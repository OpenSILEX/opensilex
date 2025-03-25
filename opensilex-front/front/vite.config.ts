import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import vueDevTools from 'vite-plugin-vue-devtools';
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';

export default defineConfig({
  resolve: {
    alias : {
      '~bootstrap': resolve(__dirname, 'node_modules/bootstrap')
    }
  },

    // optimizeDeps: {
    //   include: ['vue', 'vue-router']
    // },
  
  plugins: [vue(), vueDevTools(), VueI18nPlugin({
    defaultSFCLang: "yaml"
  })], // Utilisez le plugin Vue pour Vite
  // css: {
  //   preprocessorOptions: {
  //     scss: {
  //       // additionalData: `@import "/theme.scss";`
  //     }
  //   }
  // },
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