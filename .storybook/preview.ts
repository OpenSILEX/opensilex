// .storybook/preview.ts
import type { Preview } from '@storybook/vue3';
import { setup } from '@storybook/vue3';
import { createI18n } from 'vue-i18n';
import en from '../opensilex-front/front/src/lang/message-en.json';
import fr from '../opensilex-front/front/src/lang/message-fr.json';

const i18n = createI18n({
  legacy: false,       // required for Composition API / useI18n()
  locale: 'en',
  fallbackLocale: 'en',
  messages: { en, fr },
});

setup((app) => {
  app.use(i18n);
});

const preview: Preview = {
  parameters: {
    actions: {},
    controls: { expanded: true },
  },
};

export default preview;