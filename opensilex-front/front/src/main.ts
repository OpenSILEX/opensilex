/// <reference path="../../../opensilex/front/types/opensilex.d.ts" />
import "reflect-metadata"
import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { Container } from 'inversify'
import IHttpClient from './lib/IHttpClient'
import HttpClient from './lib/HttpClient'
import { IAPIConfiguration, ApiServiceBinder, FrontService, FrontConfigDTO } from './lib'
import { ModuleLoader } from './models/ModuleLoader'
import { SecurityService } from 'opensilex/index'
import { OpenSilexPlugin } from './models/OpenSilexPlugin'

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

let opensilex = new OpenSilexPlugin(baseApi);
Vue.use(opensilex);

// Load application configuration
const frontService = opensilex.getService<FrontService>("FrontService");

let moduleLoader = new ModuleLoader(DEV_BASE_API_PATH, frontService);

frontService.getConfig()
  .then(function (config: FrontConfigDTO) {
    console.log(config);

    moduleLoader.loadComponentModules([
      "opensilex#test"
    ]).then(function () {
      const securityService = opensilex.getService<SecurityService>("SecurityService");
      console.log(securityService);
      securityService.getAccestList()
    }).catch(console.error);

    // TODO Check user

    new Vue({
      router,
      store,
      render: h => h(App)
    }).$mount('#app')
  })
  .catch(console.error);
