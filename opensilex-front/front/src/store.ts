import Vue from 'vue'
import Vuex, { Store } from 'vuex'
import { User } from '@/models/User'
import VueRouter, { Route } from 'vue-router';
import { FrontConfigDTO, RouteDTO, MenuItemDTO } from './lib';
import { OpenSilexVuePlugin } from './models/OpenSilexVuePlugin';
import { ModuleComponentDefinition } from './models/ModuleComponentDefinition';
import { OpenSilexRouter } from './models/OpenSilexRouter';

Vue.use(Vuex)
Vue.use(VueRouter)

let expireTimeout: any = undefined;
let loaderCount: number = 0;

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false,
    openSilexRouter: new OpenSilexRouter(),
    menu: {}
  },
  mutations: {
    login(state, user: User) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      expireTimeout = setTimeout(() => {
        let method: any = "logout";
        this.commit(method);
      }, user.getExpirationMs());

      state.user = user;
      state.openSilexRouter.resetRouter(state.user);
      state.menu = state.openSilexRouter.getMenu();
    },
    logout(state) {
      if (expireTimeout != undefined) {
        clearTimeout(expireTimeout);
        expireTimeout = undefined;
      }

      state.user = User.logout();
      state.openSilexRouter.resetRouter(state.user);
      state.menu = state.openSilexRouter.getMenu();
    },
    setConfig(state, config: FrontConfigDTO) {
      state.openSilexRouter.setConfig(config);
    },
    showLoader(state) {
      if (loaderCount == 0) {
        state.loaderVisible = true;
      }
      loaderCount++;
    },
    hideLoader(state) {
      loaderCount--;
      if (loaderCount == 0) {
        state.loaderVisible = false
      }
    }
  },
  actions: {
  },
  modules: {
  }
})

