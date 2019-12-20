/// <reference path="../../../opensilex/front/types/opensilex.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />
/**
 * CHANGE THIS VARIABLE IF NEEDED TO CHANGE API ENDPOINT
 */
const DEV_BASE_API_PATH = "http://localhost:8666/rest";

import "reflect-metadata"

// Allow access to global "document" variable
declare var document: any;

// Import Vue as a global window variable
import Vue, { Component } from 'vue'
declare var window: any;
window.Vue = Vue;

Vue.config.productionTip = false;

// Import and assignation to enable auto rebuild on ws library change
import * as LATEST_UPDATE from "./opensilex.dev";
Vue.prototype.LATEST_UPDATE = LATEST_UPDATE.default

// Define if script in debug mode
let isDebug = false;
if (window["webpackHotUpdate"]) {
  isDebug = true;
}

// Initialise logger
console.log = console.log || function() {};
console.warn = console.warn || console.log;
console.error = console.error || console.log;
if (isDebug) {
  console.debug = console.debug || console.log;
} else {
  console.debug = function() {};
}
console.debug("Logger initialized in debug mode");

// Initialize service API container
let baseApi = DEV_BASE_API_PATH;
if (isDebug) {
  console.warn(
    'Vue is running in development mode, with base API set by default to ' + DEV_BASE_API_PATH + '\n' +
    'If you start your webservices server with another host or port configuration,\n' +
    'please edit opensilex-front/front/src/main.ts and update DEV_BASE_API_PATH constant'
  );
} else {
  let splitURI = window.location.href.split("/");
  baseApi = splitURI[0] + "//" + splitURI[2] + "/rest"
}

// Setup store imports
import store from './models/Store'

// Local imports
console.debug("Import local files...");
import App from './App.vue'
import { FrontConfigDTO, FrontService } from './lib'
import HttpResponse from './lib/HttpResponse'
import { User } from './models/User'
import { ModuleComponentDefinition } from './models/ModuleComponentDefinition'
import { OpenSilexVuePlugin } from './models/OpenSilexVuePlugin'
console.debug("Local file imports done !");

// Initialize cookie management library
console.debug("Initialize Cookie plugin...");
import VueCookies from 'vue-cookies'
Vue.use(VueCookies);
console.debug("Cookie plugin initialized !");

// Initialise bootstrap
console.debug("Initialize Bootstrap plugin...");
import BootstrapVue from 'bootstrap-vue'
Vue.use(BootstrapVue);
console.debug("Bootstrap plugin initialized !");

// Initialise font awesome
console.debug("Initialize FontAwesomeIcon plugin...");
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { library } from '@fortawesome/fontawesome-svg-core'
import { faPowerOff, faTimes, faTrashAlt, faEdit} from '@fortawesome/free-solid-svg-icons'
import {  } from '@fortawesome/free-solid-svg-icons'
import { }from '@fortawesome/free-solid-svg-icons'
library.add(faPowerOff, faTimes, faTrashAlt, faEdit);
Vue.component('font-awesome-icon', FontAwesomeIcon)
console.debug("FontAwesomeIcon plugin initialized !");

// Initialize i18n
import VueI18n from 'vue-i18n'
Vue.use(VueI18n)

// Enable Vue front plugin manager for OpenSilex API
console.debug("Enable OpenSilex plugin...");
let $opensilex = new OpenSilexVuePlugin(baseApi, store);
Vue.use($opensilex);
console.debug("OpenSilex plugin enabled !");

// Define global error manager
console.debug("Define global error manager");
const manageError = function manageError(error) {
  console.error(error);
  document.getElementById('opensilex-error-loading').style.visibility = 'visible';
}

// Load default components
console.debug("Load default components...");
import components from './components';
for (let componentName in components) {
  console.debug("Load default component", componentName);
  Vue.component(componentName, components[componentName]);
}

// Load d3
import * as d3 from 'd3'

$opensilex.initAsyncComponents(components)
  .then(() => {
    console.debug("Default components loaded !");

    // Get OpenSilex configuration
    console.debug("Start loading configuration...");
    const frontService = $opensilex.getService<FrontService>("FrontService");
    frontService.getConfig()
      .then(function (configResponse) {
        const config: FrontConfigDTO = configResponse.response.result;
        store.commit("setConfig", config);
        console.debug("Configuration loaded", config);

        let urlParams = new URLSearchParams(window.location.search);
        console.debug("Read url parameters", urlParams);

        // Load only necessary component if application is embed in an iframe
        let embed = urlParams.has('embed');

        if (embed) {
          console.debug("Application is embed");
        } else {
          console.debug("Application is not embed");
        }

        // Define user
        console.debug("Define current user...");
        let user: User | undefined = undefined;
        if (urlParams.has("token")) {
          let token = urlParams.get("token");
          console.debug("Try to load user from URL token...");
          if (token != null) {
            user = User.fromToken(token);
            console.debug("User sucessfully loaded from URL token !");
          }
        }

        if (user == undefined) {
          console.debug("Try to load user from cookie...");
          user = User.fromCookie();
          console.debug("User sucessfully loaded from cookie !");
        }

        if (!user.isLoggedIn()) {
          console.debug("User is ANONYMOUS !");
        } else {
          console.debug("User is:", user.getEmail());
        }
        // Init user
        console.debug("Initialize global user");
        store.commit("login", user);

        // Init routing
        console.debug("Initialize routing");
        let router = store.state.openSilexRouter.getRouter();

        // Initalise main layout components from configuration
        console.debug("Define initial modules to load...");
        let modulesToLoad: ModuleComponentDefinition[] = [
          ModuleComponentDefinition.fromString(config.homeComponent),
          ModuleComponentDefinition.fromString(config.notFoundComponent)
        ];

        if (!embed) {
          console.debug("Application is not embed");
          modulesToLoad = modulesToLoad.concat([
            ModuleComponentDefinition.fromString(config.footerComponent),
            ModuleComponentDefinition.fromString(config.headerComponent),
            ModuleComponentDefinition.fromString(config.loginComponent),
            ModuleComponentDefinition.fromString(config.menuComponent)
          ]);
        } else {
          console.debug("Application is embed");
        }

        $opensilex.loadComponentModules(modulesToLoad)
          .then(() => {
            // Initialize main application rendering
            console.debug("Initialize main application rendering");
            new Vue({
              router,
              store,
              render: h => h(App, {
                props: {
                  embed: embed,
                  footerComponent: config.footerComponent,
                  headerComponent: config.headerComponent,
                  loginComponent: config.loginComponent,
                  menuComponent: config.menuComponent
                }
              },
              )
            }).$mount('#app').$nextTick(() => {
              // Hide loader
              console.debug("Hide application init loader");
              document.getElementById('opensilex-loader').style.visibility = 'hidden';
            });
          })
      })
  })