import { ApiServiceBinder } from './lib';
import ImageView from './components/images/ImageView.vue';
import ImageSearch from './components/images/ImageSearch.vue';
import ImageList from './components/images/ImageList.vue';
import ImageGrid from './components/images/viewComponents/ImageGrid.vue';
import ImageSingle from './components/images/viewComponents/ImageSingle.vue';
import ImageCarousel from './components/images/viewComponents/ImageCarousel.vue';
import SciObjectURISearch from './components/images/searchComponents/SciObjectURISearch.vue';
import SciObjectAliasSearch from './components/images/searchComponents/SciObjectAliasSearch.vue';
import ImageTypeSearch from './components/images/searchComponents/ImageTypeSearch.vue';
import ExperimentSearch from './components/images/searchComponents/ExperimentSearch.vue';
import SciObjectTypeSearch from './components/images/searchComponents/SciObjectTypeSearch.vue';
import TimeSearch from './components/images/searchComponents/TimeSearch.vue';

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
        // TODO register components
    },
    components: {
        'phis-ImageView': ImageView,
        'phis-ImageSearch': ImageSearch,
        'phis-ImageList': ImageList,
        'phis-ImageGrid': ImageGrid,
        'phis-ImageSingle': ImageSingle,
        'phis-ImageCarousel': ImageCarousel,
        'phis-SciObjectURISearch': SciObjectURISearch,
        'phis-SciObjectAliasSearch': SciObjectAliasSearch,
        'phis-ImageTypeSearch': ImageTypeSearch,
        'phis-ExperimentSearch': ExperimentSearch,
        'phis-SciObjectTypeSearch': SciObjectTypeSearch,
        'phis-TimeSearch': TimeSearch
    }
};
