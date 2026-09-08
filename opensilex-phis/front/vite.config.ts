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
      // Le backend (FrontAPI#getModuleFrontLibFileName) sert toujours "<moduleId>.umd.min.js",
      // quel que soit le format buildé : le nom de fichier doit donc matcher cette convention.
      fileName: (format) => `opensilex-phis.${format}.min.js`, // Nom du fichier de sortie
      formats: ['es', 'umd'], // Formats de sortie (ES Module et UMD)
    },
    rollupOptions: {
      // Externaliser les dépendances (si nécessaire)
      external: ['vue'],
      output: {
        globals: {
          vue: 'Vue',
        },
      },
    },
  },
  base: '/lib/', // Chemin de base pour les ressources
});