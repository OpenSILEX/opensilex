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
let renewTokenOnEvent = function () {
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
      $opensilex.$store.commit("login", $opensilex.user);
    });
}

let defaultConfig: FrontConfigDTO = {
  homeComponent: "opensilex-front.ToDoComponent",
  notFoundComponent: "opensilex-front.ToDoComponent",
  headerComponent: "opensilex-front.ToDoComponent",
  loginComponent: "opensilex-front.ToDoComponent",
  menuComponent: "opensilex-front.ToDoComponent",
  footerComponent: "opensilex-front.ToDoComponent",
  menu: [],
  routes: []
};

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false,
    openSilexRouter: new OpenSilexRouter(),
    config: defaultConfig,
    menu: menu
  },
  mutations: {
    login(state, user: User) {
      console.debug("Login", user);

      if (expireTimeout != undefined) {
        console.debug("Clear token timeout");
        clearTimeout(expireTimeout);
        expireTimeout = undefined;

        console.debug("Clear renew timeout");
        clearTimeout(autoRenewTimeout);
        autoRenewTimeout = undefined;
      }

      console.debug("Define expiration timeout");
      let exipreAt = user.getExpirationMs();
      expireTimeout = setTimeout(() => {
        let method: any = "logout";
        this.commit(method);
      }, exipreAt);

      let autoRenewDelay = exipreAt - 60000;
      if (autoRenewDelay > 0) {
        console.debug("Enable renew timeout in", autoRenewDelay, "ms");
        autoRenewTimeout = setTimeout(() => {
          // TODO display toast to warn user that 
          renewStarted = false;
          console.debug("Enable renew event listeners");
          document.addEventListener('mousemove ', renewTokenOnEvent);
          document.addEventListener('click ', renewTokenOnEvent);
          document.addEventListener('keydown', renewTokenOnEvent);
        }, autoRenewDelay);
      }

      console.debug("Define user");
      state.user = user;
      console.debug("Reset router");
      state.openSilexRouter.resetRouter(state.user);
      console.debug("Reset menu");
      state.menu = state.openSilexRouter.getMenu();
    },
    logout(state) {
      console.debug("Logout");

      if (expireTimeout != undefined) {
        console.debug("Clear token timeout");
        clearTimeout(expireTimeout);
        expireTimeout = undefined;

        console.debug("Clear renew timeout");
        clearTimeout(autoRenewTimeout);
        autoRenewTimeout = undefined;
      }

      console.debug("Set user to anonymous");
      state.user = User.logout();
      console.debug("Reset router");
      state.openSilexRouter.resetRouter(state.user);
      console.debug("Reset menu");
      state.menu = state.openSilexRouter.getMenu();
    },
    setConfig(state, config: FrontConfigDTO) {
      state.config = config;
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
    },
    refresh(state) {
      state.openSilexRouter.refresh();
    }
  },
  actions: {
  },
  modules: {
  }
})

