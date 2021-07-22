
//Elasticsearch
import ElasticSearchBar from "./components/elastic/ElasticSearchBar.vue" ;    
import ElasticHeaderComponent from "./components/layout/ElasticHeaderComponent.vue" ;    




//import { ApiServiceBinder } from './lib';
export default {
    install(Vue, options) {
       // ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    },
    components: {
        "opensilex-elastic-ElasticSearchBar": ElasticSearchBar,
        "opensilex-elastic-ElasticHeaderComponent" : ElasticHeaderComponent
    }


};