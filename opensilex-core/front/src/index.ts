import {VueConstructor} from "vue";
import Test from "./components/Test.vue"
import libs from "./opensilex.lib"

export default {
    install(vue: VueConstructor, options: any) {
        vue.component("Test", Test);
    }
};