import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()], // Utilisez le plugin Vue pour Vite
  build: {
    outDir: 'dist', // Dossier de sortie
    minify: true,
    lib: {
      entry: resolve(__dirname, 'src/index.ts'), // Point d'entrée
      name: 'opensilex-phis', // Nom de la bibliothèque
      fileName: (format) => `opensilex-phis.${format}.min.js`, // Nom du fichier de sortie
      formats: ['es', 'umd'], // Formats de sortie (ES Module et UMD)
    },
    rollupOptions: {
      // Externaliser les dépendances (si nécessaire)
      external: ['vue', 'vue-i18n'],
      output: {
        globals: {
          vue: 'Vue',
          // Must match window.VueI18n set in opensilex-front/front/src/main.ts.
          // Without this mapping rollup falls back to a guessed "vue_i18n" global
          // that does not exist, and the UMD bundle throws while being evaluated.
          'vue-i18n': 'VueI18n',
        },
      },
    },
  },
  base: '/lib/', // Chemin de base pour les ressources
});