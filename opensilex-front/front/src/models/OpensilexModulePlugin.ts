import type { Plugin, ObjectPlugin, Component } from 'vue';
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";

/**
 * Type for OpenSILEX's module.
 * Each module should have an index.ts file exporting an OpensilexModulePlugin (Vue plugin with map components).
 */
export type OpensilexModulePlugin = Plugin<OpensilexPluginOptions> & {
  components?: OpensilexPluginComponentMap,
  lang?: Record<string, any>
};

export type OpensilexPluginOptions = {opensilexInstance: OpenSilexVuePlugin}
export type OpensilexPluginComponentMap = Record<string, Component>;

/**
 * Runtime type guard for {@link OpensilexModulePlugin}: a module export is a valid plugin if it is
 * either an install function or an object exposing an `install` method, as required by Vue's
 * `Plugin` union (`FunctionPlugin | ObjectPlugin`).
 */
export function isOpensilexModulePlugin(value: unknown): value is OpensilexModulePlugin {
  if (typeof value === "function") {
    return true;
  }

  return typeof value === "object"
      && value !== null
      && typeof (value as ObjectPlugin).install === "function";
}
