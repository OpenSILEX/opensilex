/// <reference path="../../../opensilex/front/types/opensilex.d.ts" />
import { SecurityService } from 'opensilex/index'
import "reflect-metadata"
import Vue from 'vue'
import App from './App.vue'
import { FrontConfigDTO, FrontService } from './lib'
import { ModuleLoader } from './modules/ModuleLoader'
import { ModuleFrontVuePlugin } from './modules/ModuleFrontVuePlugin'
import router from './router'
import store from './store'

import * as IGNORE_ME from "./opensilex.dev";
console.log(IGNORE_ME.default);

declare var document: any;

Vue.config.productionTip = false

// Initialize service API container
const DEV_BASE_API_PATH = "http://localhost:8666/rest";

let baseApi = DEV_BASE_API_PATH;
if (window["webpackHotUpdate"]) {
  console.warn(
    'Vue is running in development mode, with base API set by default to ' + DEV_BASE_API_PATH + '\n' +
    'If you start your webservices server with another host or port configuration,\n' +
    'please edit opensilex-front/front/src/main.ts and update DEV_BASE_API_PATH constant'
  );
} else {
  let splitURI = window.location.href.split("/");
  baseApi = splitURI[0] + "//" + splitURI[2] + "/rest/"
}

let frontPlugin = new ModuleFrontVuePlugin(baseApi);
Vue.use(frontPlugin);

// Load application configuration
const frontService = frontPlugin.getService<FrontService>("FrontService");

let moduleLoader = new ModuleLoader(DEV_BASE_API_PATH, frontService);

frontService.getConfig()
  .then(function (config: FrontConfigDTO) {
    console.log(config);

    moduleLoader.loadModules([
      "opensilex"
    ]).then(function () {
      const securityService = frontPlugin.getService<SecurityService>("SecurityService");
      console.log(securityService);
      securityService.getAccestList();

      // TODO Check user access and rights

      new Vue({
        router,
        store,
        render: h => h(App)
      }).$mount('#app')

      document.getElementById('opensilex-loader').style.visibility = 'hidden';
    }).catch(console.error);
  })
  .catch(console.error);
