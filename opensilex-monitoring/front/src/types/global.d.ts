import type { OpenSilexTranslator } from './OpenSilexPlugin';

/**
 * Every template in this module calls the global `$t(...)` helper the host's vue-i18n instance
 * installs on `app.config.globalProperties`. The real augmentation lives in vue-i18n's own types,
 * which — like the plugin itself — are not resolvable from an extension module's `front/`
 * directory. Declaring it here once is what lets every component's template be type-checked
 * instead of every `$t` call being an unrecognised global.
 */
declare module 'vue' {
    interface ComponentCustomProperties {
        $t: OpenSilexTranslator['t'];
    }
}

export {};
