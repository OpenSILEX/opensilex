import PhisLoginComponent from "./components/layout/PhisLoginComponent.vue";
import PhisHeaderComponent from "./components/layout/PhisHeaderComponent.vue";

const components = {
    "opensilex-phis-PhisLoginComponent": PhisLoginComponent,
    "opensilex-phis-PhisHeaderComponent": PhisHeaderComponent
};

export default {
    install(app, options) {
        for (const componentName in components) {
            app.component(componentName, components[componentName]);
        }
    },
    components
};
