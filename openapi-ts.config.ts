import { defineConfig } from '@hey-api/openapi-ts';
import { resolve } from 'path';

export default defineConfig({
  input: resolve(__dirname, 'opensilex-front/front/src/lib/openapi.json'),
  output: resolve(__dirname, 'opensilex-front/front/src/lib/generated'),
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    '@hey-api/sdk'
  ]
});
