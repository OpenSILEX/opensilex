// Import the binder directly rather than through './lib': the barrel re-exports HttpClient, which
// pulls in whatwg-fetch and drags roughly 90 KB of polyfill into the bundle. The host has already
// bound an HTTP client into the container, so the module never needs its own.
import { ApiServiceBinder } from './lib/ApiServiceBinder';

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
const components: { [key: string]: any } = {
    'opensilex-monitoring-MonitoringView': MonitoringView,
    'opensilex-monitoring-MonitoringBanner': MonitoringBanner,
    'opensilex-monitoring-DatabaseStatusTile': DatabaseStatusTile,
    'opensilex-monitoring-ConnectedUsersPanel': ConnectedUsersPanel,
    'opensilex-monitoring-PeriodSelector': PeriodSelector,
    'opensilex-monitoring-ActivityChart': ActivityChart,
    'opensilex-monitoring-FailureRateCard': FailureRateCard,
    'opensilex-monitoring-RequestLogTable': RequestLogTable,
};

const messages: { [locale: string]: any } = { fr, en };

const plugin = {
    install(app: any, options: any) {
        const $opensilex = app.$opensilex ?? app.config.globalProperties.$opensilex;
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
