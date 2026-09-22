// Import the binder directly rather than through './lib': the barrel re-exports HttpClient, which
// pulls in whatwg-fetch and drags roughly 90 KB of polyfill into the bundle. The host has already
// bound an HTTP client into the container, so the module never needs its own.
import { ApiServiceBinder } from './lib/ApiServiceBinder';

import type { App, Component, Plugin } from 'vue';
import type { OpenSilexPlugin } from './types/OpenSilexPlugin';

import MonitoringView from './components/MonitoringView.vue';
import MonitoringBanner from './components/MonitoringBanner.vue';
import DatabaseStatusTile from './components/DatabaseStatusTile.vue';
import ConnectedUsersPanel from './components/ConnectedUsersPanel.vue';
import PeriodSelector from './components/PeriodSelector.vue';
import ActivityChart from './components/ActivityChart.vue';
import FailureRateCard from './components/FailureRateCard.vue';
import RequestLogTable from './components/RequestLogTable.vue';

import fr from './lang/monitoring-fr.json';
import en from './lang/monitoring-en.json';

/**
 * Component identifiers follow the `{module}-{Name}` convention the router relies on to know which
 * bundle to load. The router splits on '-' and pops the last segment as the component name, so the
 * name itself must carry no dash of its own.
 */
const components: Record<string, Component> = {
    'opensilex-monitoring-MonitoringView': MonitoringView,
    'opensilex-monitoring-MonitoringBanner': MonitoringBanner,
    'opensilex-monitoring-DatabaseStatusTile': DatabaseStatusTile,
    'opensilex-monitoring-ConnectedUsersPanel': ConnectedUsersPanel,
    'opensilex-monitoring-PeriodSelector': PeriodSelector,
    'opensilex-monitoring-ActivityChart': ActivityChart,
    'opensilex-monitoring-FailureRateCard': FailureRateCard,
    'opensilex-monitoring-RequestLogTable': RequestLogTable,
};

type LocaleMessages = Record<string, unknown>;
const messages: Record<string, LocaleMessages> = { fr, en };

/**
 * The host exposes `$opensilex` two different ways depending on how it wires the app instance,
 * and neither is part of Vue's own `App` type — hence the narrow, local cast instead of `any`.
 */
function getOpenSilex(app: App): OpenSilexPlugin {
    const withOwnProperty = app as unknown as { $opensilex?: OpenSilexPlugin };
    const withGlobalProperties = app.config.globalProperties as unknown as { $opensilex: OpenSilexPlugin };
    return withOwnProperty.$opensilex ?? withGlobalProperties.$opensilex;
}

const plugin: Plugin = {
    install(app: App) {
        const $opensilex = getOpenSilex(app);
        ApiServiceBinder.with($opensilex.getServiceContainer());

        // Registered here rather than through a `components` field: the host loads a module by
        // calling `app.use(plugin)` and nothing else, so registration is the plugin's own job.
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
