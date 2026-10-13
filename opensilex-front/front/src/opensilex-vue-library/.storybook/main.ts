import type { StorybookConfig } from "@storybook/vue3-vite";
import vue from "@vitejs/plugin-vue";

const config: StorybookConfig = {
  stories: ["../stories/**/*.stories.@(js|ts)"],
  framework: "@storybook/vue3-vite",
  // le preset vue3-vite n'ajoute pas le plugin Vue : on l'ajoute ici
  // (Storybook est lancé depuis ce dossier, sans le vite.config.ts de l'app)
  viteFinal: async (config) => {
    config.plugins = [...(config.plugins || []), vue()];
    return config;
  },
};

export default config;
