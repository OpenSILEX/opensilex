import type { Plugin, ObjectPlugin, Component } from 'vue';

/**
 * Type for OpenSILEX's module.
 * Each module should have an index.ts file exporting an OpensilexModulePlugin (Vue plugin with map components).
 */
export type OpensilexModulePlugin = Plugin & {
  components: OpensilexModuleComponentMap
};

export type OpensilexModuleComponentMap = Record<string, Component>;
