import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()], // Utilisez le plugin Vue pour Vite
  build: {
    outDir: 'dist', // Dossier de sortie
    minify: true,
    lib: {
      entry: resolve(__dirname, 'src/lib/index.ts'), // Point d'entrée
      name: 'opensilex-security', // Nom de la bibliothèque
      fileName: (format) => `opensilex-security.${format}.min.js`, // Nom du fichier de sortie
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
  experimental: {
    renderBuiltUrl(filename, { hostType }) {
      if (hostType === 'js') {
        return { runtime: `window.__toCdnUrl(${JSON.stringify(filename)})` }
      } else {
        return { relative: true }
      }
    },
  },
});