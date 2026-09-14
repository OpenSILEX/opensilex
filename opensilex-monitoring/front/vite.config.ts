import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  // ECharts guards its development-only checks behind `process.env.NODE_ENV`, and Vite's library
  // mode does not substitute that the way an application build does. Left alone, the bundle throws
  // `ReferenceError: process is not defined` the moment the host injects it: the <script> load event
  // still fires, so the loader believes the module arrived, reads back the placeholder it had put on
  // the global, calls app.use() on it to no effect, and every component of the module silently fails
  // to resolve. Substituting it here also lets the minifier drop those branches.
  define: {
    'process.env.NODE_ENV': JSON.stringify('production'),
  },
  build: {
    outDir: 'dist',
    minify: true,
    // FrontAPI only ever serves <module>.umd.min.js, so a side chunk emitted next to it would 404
    // at runtime. One file, always.
    codeSplitting: false,
    lib: {
      entry: new URL('src/index.ts', import.meta.url).pathname,
      name: 'opensilex-monitoring',
      fileName: (format) => `opensilex-monitoring.${format}.min.js`,
      formats: ['es', 'umd'],
    },
    rollupOptions: {
      // The host page owns the Vue runtime; a second copy would break provide/inject.
      // ECharts is deliberately NOT external: there is no window.echarts for the host to hand us,
      // and declaring it here as a module dependency keeps the rest of the monorepo out of its
      // version bumps.
      external: ['vue'],
      output: {
        // The host loads the bundle with a <script> tag and then calls `app.use(window[moduleId])`,
        // so the global has to be the plugin itself.
        exports: 'default',
        globals: {
          vue: 'Vue',
        },
      },
    },
  },
});
