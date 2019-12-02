import "reflect-metadata"
import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { Container } from 'inversify'
import IHttpClient from './lib/IHttpClient'
import HttpClient from './lib/HttpClient'
import { IAPIConfiguration, ApiServiceBinder, FrontService, FrontConfigDTO } from './lib'

Vue.config.productionTip = false

// Initialize service API container
const DEV_BASE_API_PATH = "http://localhost:8666/rest";

let container = new Container();

container.bind<IHttpClient>("IApiHttpClient").to(HttpClient).inSingletonScope();
if (window["webpackHotUpdate"]) {
  console.warn(
    'Vue is running in development mode, with base API set by default to ' + DEV_BASE_API_PATH + '\n' +
    'If you start your webservices server with another host or port configuration,\n' +
    'please edit opensilex-front/front/src/main.ts and update DEV_BASE_API_PATH constant'
  );
  container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({
    basePath: DEV_BASE_API_PATH
  });
} else {
  let splitURI = window.location.href.split("/");
  let baseAPI = splitURI[0] + "//" + splitURI[2] + "/rest/"
  container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({
    basePath: baseAPI
  });
}

ApiServiceBinder.with(container);

// Load application configuration
const frontService = container.get<FrontService>("FrontService");
frontService.getConfig()
  .then(function (config: FrontConfigDTO) {
    console.log(config);

    // TODO Check user
    
    new Vue({
      router,
      store,
      render: h => h(App)
    }).$mount('#app')
  })
  .catch(console.error);
