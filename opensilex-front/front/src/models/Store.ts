import Vue from 'vue'
import Vuex from 'vuex'
import { User } from '@/models/User'
import VueRouter from 'vue-router';
import { FrontConfigDTO, MenuItemDTO } from '../lib';
import { OpenSilexRouter } from './OpenSilexRouter';
import { SecurityService } from 'opensilex/index';
import { OpenSilexVuePlugin } from './OpenSilexVuePlugin';

Vue.use(Vuex)
Vue.use(VueRouter)

let expireTimeout: any = undefined;
let autoRenewTimeout: any = undefined;
let loaderCount: number = 0;
let menu: Array<MenuItemDTO> = [];

let renewStarted = false;
let renewTokenOnEvent= function() {
  console.log("Disable renew event listeners");
  document.removeEventListener('mousemove ', renewTokenOnEvent);
  document.removeEventListener('click ', renewTokenOnEvent);
  document.removeEventListener('keydown', renewTokenOnEvent);

  if (!renewStarted) {
    renewStarted = true;
  } else {
    console.log("Ignore renew event");
    return;
  }

  console.log("Start token renew call");

  let $opensilex: OpenSilexVuePlugin = Vue["$opensilex"];
  $opensilex.getService<SecurityService>("opensilex.SecurityService")
    .renewToken($opensilex.user.getToken())
    .then((newToken) => {
      console.log("Token renewed", newToken);
      $opensilex.user.setToken(newToken);
      $opensilex.store.commit("login", $opensilex.user);
    });
}

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false,
    openSilexRouter: new OpenSilexRouter(),
    menu: menu
  },
  mutations: {
    login(state, user: User) {
      if (expireTimeout != undefined) {
        console.log("Clear renew timeout");
        clearTimeout(expireTimeout);
        clearTimeout(autoRenewTimeout);
        expireTimeout = undefined;
        autoRenewTimeout = undefined;
      }

      let exipreAt = user.getExpirationMs();
      expireTimeout = setTimeout(() => {
        let method: any = "logout";
        this.commit(method);
      }, exipreAt);

      let autoRenewDelay = exipreAt - 60000;
      if (autoRenewDelay > 0) {
        console.log("Enable renew timeout in", autoRenewDelay, "ms");
        autoRenewTimeout = setTimeout(() => {
          // TODO display toast to warn user that 
          renewStarted = false;
          console.log("Enable renew event listeners");
          document.addEventListener('mousemove ', renewTokenOnEvent);
          document.addEventListener('click ', renewTokenOnEvent);
          document.addEventListener('keydown', renewTokenOnEvent);
        }, autoRenewDelay);
      }

      state.user = user;
      state.openSilexRouter.resetRouter(state.user);
      state.menu = state.openSilexRouter.getMenu();
    },
    logout(state) {
      if (expireTimeout != undefined) {
        console.log("Clear renew timeout");
        clearTimeout(expireTimeout);
        clearTimeout(autoRenewTimeout);
        expireTimeout = undefined;
        autoRenewTimeout = undefined;
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

