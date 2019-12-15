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

declare var window: any;

let expireTimeout: any = undefined;
let autoRenewTimeout: any = undefined;
let loaderCount: number = 0;
let menu: Array<MenuItemDTO> = [];
let inactivityRenewTimeoutInMin = 1;
let renewStarted = false;
let currentUser = undefined;
let renewTokenOnEvent = function (event) {
  if (event && event.keyCode
    && (
      event.ctrlKey // Crtl key pressed
      || event.altKey // Alt key pressed key
      || event.shiftKey // Shift key pressed
      || event.metaKey // Meta key pressed
    )) {
    // If a modifier key is pressed don't consider it as a renewal activity sequence
    return;
  }
  console.log("Disable renew event listeners");
  window.removeEventListener('mousemove', renewTokenOnEvent);
  window.removeEventListener('click', renewTokenOnEvent);
  window.removeEventListener('keydown', renewTokenOnEvent);

  if (!renewStarted) {
    renewStarted = true;
  } else {
    console.log("Ignore renew event");
    return;
  }

  let $opensilex: OpenSilexVuePlugin = Vue["$opensilex"];

  $opensilex.getService<SecurityService>("opensilex.SecurityService")
    .renewToken(currentUser.getToken())
    .then((sucess) => {
      console.log("Token renewed", sucess.response.result);
      currentUser.setToken(sucess.response.result);
      $opensilex.$store.commit("login", currentUser);
    })
    .catch(console.error);
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

      let exipreAfter = user.getExpirationMs();
      console.debug("Define expiration timeout", exipreAfter);
      expireTimeout = setTimeout(() => {
        console.debug("Automatically call logout");
        let method: any = "logout";
        this.commit(method);
      }, exipreAfter);

      let inactivityRenewDelay = user.getInactivityRenewDelayMs();
      if (inactivityRenewDelay > 0) {
        console.debug("Enable inactivity renew timeout in", inactivityRenewDelay, "ms");
        autoRenewTimeout = setTimeout(() => {
          // TODO display toast to warn user that is session will be distoryed if no activity
          renewStarted = false;
          console.debug("Enable renew event listeners");
          window.addEventListener('mousemove', renewTokenOnEvent);
          window.addEventListener('click', renewTokenOnEvent);
          window.addEventListener('keydown', renewTokenOnEvent);
        }, inactivityRenewDelay);
      } 

      if (!user.needRenew()) {
        console.debug("Define user");
        currentUser = user;
        state.user = user;
        console.debug("Reset router");
        state.openSilexRouter.resetRouter(state.user);
        console.debug("Reset menu");
        state.menu = state.openSilexRouter.getMenu();
        console.log(state.menu);
      }
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
        console.log("Disable renew event listeners");
        window.removeEventListener('mousemove', renewTokenOnEvent);
        window.removeEventListener('click', renewTokenOnEvent);
        window.removeEventListener('keydown', renewTokenOnEvent);
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
});
