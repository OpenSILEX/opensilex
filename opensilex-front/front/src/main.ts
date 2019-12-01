import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { Container } from 'inversify'
import IHttpClient from './lib/IHttpClient'
import HttpClient from './lib/HttpClient'
import { IAPIConfiguration, ApiServiceBinder } from './lib'

Vue.config.productionTip = false

let container = new Container();
container.bind<IHttpClient>("IHttpClient").to(HttpClient).inSingletonScope();
container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({});
ApiServiceBinder.with(container);


new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
