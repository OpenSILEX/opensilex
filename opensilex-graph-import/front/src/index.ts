import { ApiServiceBinder } from './lib';
import GraphImportView from './components/GraphImportView.vue';
import fr from './lang/graph-import-fr.json';
import en from './lang/graph-import-en.json';
import type { App, Component } from 'vue';
import type { OpenSilexPlugin } from './opensilexPlugin';

/**
 * Component identifiers follow the `{module}-{Name}` convention the router relies on to know which
 * bundle to load, so the name must carry no dash of its own. Only the page is global: its panels are
 * imported where they are used.
 */
const components: { [id: string]: Component } = {
    'opensilex-graph-import-GraphImportView': GraphImportView,
};

const messages: { [locale: string]: object } = { fr, en };

/** What the host gives a module on `app.use`: its own plugin, with the service container. */
interface HostOpenSilex extends OpenSilexPlugin {
    getServiceContainer(): Parameters<typeof ApiServiceBinder.with>[0]
    $i18n: OpenSilexPlugin['$i18n'] & { mergeLocaleMessage(locale: string, messages: object): void }
}

const plugin = {
    install(app: App & { $opensilex?: HostOpenSilex }) {
        const $opensilex: HostOpenSilex = app.$opensilex ?? app.config.globalProperties.$opensilex;
        ApiServiceBinder.with($opensilex.getServiceContainer());

        // Registered here: the host loads a module by calling `app.use(plugin)` and nothing else.
        for (const id in components) {
            app.component(id, components[id]);
        }
        for (const locale in messages) {
            $opensilex.$i18n.mergeLocaleMessage(locale, messages[locale]);
        }
    }
};

// Default export only: see the `exports: 'default'` note in vite.config.ts.
export default plugin;
