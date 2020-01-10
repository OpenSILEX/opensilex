import { ApiServiceBinder } from './lib';
import ImageView from './components/images/ImageView.vue';
import ImageSearch from './components/images/ImageSearch.vue';
import ImageGrid from './components/images/ImageGrid.vue';

export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
        // TODO register components
    },
    components: {
        'phis2ws-ImageView': ImageView,
        'phis2ws-ImageSearch': ImageSearch,
        'phis2ws-ImageGrid': ImageGrid,
    }
};
