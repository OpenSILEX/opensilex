import { ApiServiceBinder } from './lib';
import ImageView from './components/images/ImageView.vue';
import ImageSearch from './components/images/ImageSearch.vue';
import ImageGrid from './components/images/ImageGrid.vue';
import ImageSingle from './components/images/ImageSingle.vue';
import SciObjectSearch from './components/images/searchComponents/SciObjectSearch.vue';
import ImageTypeSearch from './components/images/searchComponents/ImageTypeSearch.vue';
import ExperimentSearch from './components/images/searchComponents/ExperimentSearch.vue';
import SciObjectTypeSearch from './components/images/searchComponents/SciObjectTypeSearch.vue';
import TimeSearch from './components/images/searchComponents/TimeSearch.vue';

import { library } from '@fortawesome/fontawesome-svg-core'
import { faSlidersH, faSearch } from '@fortawesome/free-solid-svg-icons'
library.add(faSlidersH,faSearch);
export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
        // TODO register components
    },
    components: {
        'phis2ws-ImageView': ImageView,
        'phis2ws-ImageSearch': ImageSearch,
        'phis2ws-ImageGrid': ImageGrid,
        'phis2ws-Image': ImageSingle,
        'phis2ws-SciObjectSearch': SciObjectSearch,
        'phis2ws-ImageTypeSearch': ImageTypeSearch,
        'phis2ws-ExperimentSearch': ExperimentSearch,
        'phis2ws-SciObjectTypeSearch': SciObjectTypeSearch,
        'phis2ws-TimeSearch': TimeSearch
    }
};
