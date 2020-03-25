import Vue from 'vue'
import Vuex from 'vuex'
import { User } from './User'
import VueRouter from 'vue-router';
import { FrontConfigDTO } from '../lib';
import { Menu } from '../models/Menu';
import { OpenSilexRouter } from './OpenSilexRouter';
import OpenSilexVuePlugin from './OpenSilexVuePlugin';
import { SecurityService } from 'opensilex-rest/index';
import { Release } from './Release';

Vue.use(Vuex)
Vue.use(VueRouter)

declare var window: any;

let expireTimeout: any = undefined;
let autoRenewTimeout: any = undefined;
let loaderCount: number = 0;
let menu: Array<Menu> = [];
let inactivityRenewTimeoutInMin = 1;
let renewStarted = false;
let currentUser = undefined;

let getOpenSilexPlugin = function(): OpenSilexVuePlugin {
  return Vue["$opensilex"];
}

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
  console.debug("Disable renew event listeners");
  window.removeEventListener('mousemove', renewTokenOnEvent);
  window.removeEventListener('click', renewTokenOnEvent);
  window.removeEventListener('keydown', renewTokenOnEvent);

  if (!renewStarted) {
    renewStarted = true;
  } else {
    console.debug("Ignore renew event");
    return;
  }

  let $opensilex: OpenSilexVuePlugin = getOpenSilexPlugin();

  $opensilex.getService<SecurityService>("opensilex-rest.SecurityService")
    .renewToken(currentUser.getAuthorizationHeader())
    .then((http) => {
      console.debug("Token renewed", http.response.result.token);
      currentUser.setToken(http.response.result.token);
      $opensilex.$store.commit("login", currentUser);
    })
    .catch(console.error);
}

let defaultConfig: FrontConfigDTO = {
  homeComponent: "opensilex-front-ToDoComponent",
  notFoundComponent: "opensilex-front-ToDoComponent",
  headerComponent: "opensilex-front-ToDoComponent",
  loginComponent: "opensilex-front-ToDoComponent",
  menuComponent: "opensilex-front-ToDoComponent",
  footerComponent: "opensilex-front-ToDoComponent",
  menu: [],
  routes: []
};

export default new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false,
    openSilexRouter: new OpenSilexRouter(),
    config: defaultConfig,
    menu: menu,
    menuVisible: true,
    disconnected: false,
    release: new Release(),
    credentials: {
      CREDENTIAL_EXPERIMENT_MODIFICATION_ID: "experiment-modification",
      CREDENTIAL_EXPERIMENT_READ_ID: "experiment-read",
      CREDENTIAL_GROUP_MODIFICATION_ID: "group-modification",
      CREDENTIAL_GROUP_DELETE_ID: "group-delete",
      CREDENTIAL_GROUP_READ_ID: "group-read",
      CREDENTIAL_PROFILE_MODIFICATION_ID: "profile-modification",
      CREDENTIAL_PROFILE_DELETE_ID: "profile-delete",
      CREDENTIAL_PROFILE_READ_ID: "profile-read",
      CREDENTIAL_PROJECT_MODIFICATION_ID: "project-modification",
      CREDENTIAL_PROJECT_DELETE_ID: "project-delete",
      CREDENTIAL_PROJECT_READ_ID: "project-read",
      CREDENTIAL_USER_MODIFICATION_ID: "user-modification",
      CREDENTIAL_USER_DELETE_ID: "user-delete",
      CREDENTIAL_USER_READ_ID: "user-read",
      CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID: "infrastructure-modification",
      CREDENTIAL_INFRASTRUCTURE_DELETE_ID: "infrastructure-delete",
      CREDENTIAL_INFRASTRUCTURE_READ_ID: "infrastructure-read",
      CREDENTIAL_VARIABLE_MODIFICATION_ID: "variable-modification",
      CREDENTIAL_VARIABLE_DELETE_ID: "variable-delete",
      CREDENTIAL_VARIABLE_READ_ID: "variable-read",
    }
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
      let expireDate = new Date(exipreAfter);
      console.debug("Define expiration timeout", expireDate.getMinutes(), "min", expireDate.getSeconds(), "sec");
      expireTimeout = setTimeout(() => {
        console.debug("Automatically call logout");
        let method: any = "logout";
        this.commit(method);
      }, exipreAfter);

      let inactivityRenewDelay = user.getInactivityRenewDelayMs();
      let inactivityRenewDelayDate = new Date(inactivityRenewDelay);
      if (inactivityRenewDelay > 0) {
        console.debug("Enable inactivity renew timeout in", inactivityRenewDelayDate.getMinutes(), "min", inactivityRenewDelayDate.getSeconds(), "sec");
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
        state.menu = Menu.fromMenuItemDTO(state.openSilexRouter.getMenu());
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
        console.debug("Disable renew event listeners");
        window.removeEventListener('mousemove', renewTokenOnEvent);
        window.removeEventListener('click', renewTokenOnEvent);
        window.removeEventListener('keydown', renewTokenOnEvent);
      }

      console.debug("Set user to anonymous");
      state.user = User.ANONYMOUS();
      getOpenSilexPlugin().clearCookie();
      state.disconnected = true;
      console.debug("Reset router");
      state.openSilexRouter.resetRouter(state.user);
      console.debug("Reset menu");
      state.menu = Menu.fromMenuItemDTO(state.openSilexRouter.getMenu());
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
    toggleMenu(state) {
      state.menuVisible = !state.menuVisible;
    },
    hideMenu(state) {
      state.menuVisible = false;
    },
    showMenu(state) {
      state.menuVisible = true;
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
