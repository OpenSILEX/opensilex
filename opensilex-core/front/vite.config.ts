import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()], // Utilisez le plugin Vue pour Vite
  build: {
    outDir: 'dist', // Dossier de sortie
    lib: {
      entry: resolve(__dirname, 'src/lib/index.ts'), // Point d'entrée
      name: 'opensilex-core', // Nom de la bibliothèque
      fileName: (format) => `opensilex-core.${format}.js`, // Nom du fichier de sortie
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
  //change port for production
  preview: {
    port: 8666,
  },
 // for dev
  server: {
    port: 8080,
  },
  base: '/lib/', // Chemin de base pour les ressources
});