import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  build: {
    outDir: 'dist',
    minify: true,
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'opensilex-graph-import',
      fileName: (format) => `opensilex-graph-import.${format}.min.js`,
      formats: ['es', 'umd'],
    },
    rollupOptions: {
      // The host page owns the Vue runtime; a second copy would break provide/inject.
      external: ['vue'],
      output: {
        // The host loads the bundle with a <script> tag and calls `app.use(window[moduleId])`, so the
        // global has to be the plugin itself, not the module namespace.
        exports: 'default',
        globals: {
          vue: 'Vue',
        },
      },
    },
  },
});
