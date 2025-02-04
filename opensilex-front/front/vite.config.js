import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
import Markdown from 'vite-plugin-vue-markdown';

export default defineConfig({
  base: process.env.NODE_ENV === 'production' ? '/osfront' : '/app',
  resolve: {
    alias: {
      vue: path.resolve(__dirname, '../../node_modules/vue/dist/vue.esm.js'),
      'opensilex-security': path.resolve(__dirname, "../../opensilex-security/front/src"),
      'opensilex-core': path.resolve(__dirname, "../../opensilex-core/front/src"),
      'opensilex-phis': path.resolve(__dirname, "../../opensilex-phis/front/src"),
    },
  },
  plugins: [
    vue(),
    Markdown(),
  ],
  define: {
    APPLICATION_VERSION: JSON.stringify(require('../../package.json').version),
  },
  server: {
    port: 8080,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },  
});
