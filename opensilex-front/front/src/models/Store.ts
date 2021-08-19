import Vue from 'vue'
import Vuex from 'vuex'
import { User } from './User'
import VueRouter from 'vue-router';
import { FrontConfigDTO } from '../lib';
import { Menu } from './Menu';
import { OpenSilexRouter } from './OpenSilexRouter';
import OpenSilexVuePlugin from './OpenSilexVuePlugin';
// @ts-ignore
import { AuthenticationService } from 'opensilex-security/index';
// @ts-ignore
import { SystemService, VersionInfoDTO } from 'opensilex-core/index';

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

let getOpenSilexPlugin = function (): OpenSilexVuePlugin {
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

  $opensilex.getService<AuthenticationService>("opensilex-security.AuthenticationService")
    .renewToken()
    .then((http) => {
      console.debug("Token renewed", http.response.result.token);
      currentUser.setToken(http.response.result.token);
      $opensilex.$store.commit("login", currentUser);
    })
    .catch(console.error);
}

let defaultConfig: FrontConfigDTO = {
  pathPrefix: "/",
  homeComponent: "opensilex-front-ToDoComponent",
  notFoundComponent: "opensilex-front-ToDoComponent",
  headerComponent: "opensilex-front-ToDoComponent",
  loginComponent: "opensilex-front-ToDoComponent",
  menuComponent: "opensilex-front-ToDoComponent",
  footerComponent: "opensilex-front-ToDoComponent",
  menu: [],
  routes: []
};

let computePage = function(router) {
  let queryParams = new URLSearchParams(window.location.search);
  let queryValues = {};
  queryParams.forEach((value, key) => {
    queryValues[key] = value;
  });
  let realRoute: any = {};
  for (let i in router.currentRoute) {
    if (i == "query") {
      realRoute.query = queryValues;
    } else {
      realRoute[i] = router.currentRoute[i];
    }
  }
  return realRoute;
}

export class SearchStore {

  results = [];
  filter = null;

}

let store = new Vuex.Store({
  state: {
    user: User.ANONYMOUS(),
    loaderVisible: false,
    openSilexRouter: null,
    config: defaultConfig,
    menu: menu,
    menuVisible: true,
    disconnected: false,
    lang: "en",
    previousPageCandidate: null,
    previousPage: [],
    search: {
      experiments: new SearchStore()
    },
    credentials: {
      CREDENTIAL_EXPERIMENT_MODIFICATION_ID: "experiment-modification",
      CREDENTIAL_EXPERIMENT_DELETE_ID: "experiment-delete",
      CREDENTIAL_GROUP_MODIFICATION_ID: "group-modification",
      CREDENTIAL_GROUP_DELETE_ID: "group-delete",
      CREDENTIAL_PROJECT_MODIFICATION_ID: "project-modification",
      CREDENTIAL_PROJECT_DELETE_ID: "project-delete",
      CREDENTIAL_USER_MODIFICATION_ID: "user-modification",
      CREDENTIAL_USER_DELETE_ID: "user-delete",
      CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID: "infrastructure-modification",
      CREDENTIAL_INFRASTRUCTURE_DELETE_ID: "infrastructure-delete",
      CREDENTIAL_VARIABLE_MODIFICATION_ID: "variable-modification",
      CREDENTIAL_VARIABLE_DELETE_ID: "variable-delete",
      CREDENTIAL_FACTOR_MODIFICATION_ID: "factor-modification",
      CREDENTIAL_FACTOR_DELETE_ID: "factor-delete",
      CREDENTIAL_GERMPLASM_MODIFICATION_ID: "germplasm-modification",
      CREDENTIAL_GERMPLASM_DELETE_ID: "germplasm-delete",
      CREDENTIAL_DOCUMENT_MODIFICATION_ID: "document-modification",
      CREDENTIAL_DOCUMENT_DELETE_ID: "document-delete",
      CREDENTIAL_AREA_MODIFICATION_ID: "area-modification",
      CREDENTIAL_AREA_DELETE_ID: "area-delete",
      CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID: "scientific-objects-modification",
      CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID: "scientific-objects-experiment-delete",
      CREDENTIAL_DEVICE_DELETE_ID: "devices-delete",
      CREDENTIAL_DEVICE_MODIFICATION_ID: "device-modification",
      CREDENTIAL_DATA_MODIFICATION_ID: "data-modification"
    }
  },
  getters: {
    language: (state) => {
      return state.lang;
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
        let opensilex = getOpenSilexPlugin();
        let message = opensilex.$i18n.t("component.common.errors.unauthorized-error");
        opensilex.showErrorToast("" + message);
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
      state.openSilexRouter = new OpenSilexRouter(config.pathPrefix);
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
      if (loaderCount < 0) {
        loaderCount = 0;
      }
    },
    toggleMenu(state) {
      setTimeout(function() {
        if (typeof(Event) === 'function') {
          // modern browsers
          window.dispatchEvent(new Event('resize'));
        } else {
          // for IE and other old browsers
          // causes deprecation warning on modern browsers
          var evt = window.document.createEvent('UIEvents'); 
          evt.initUIEvent('resize', true, false, window, 0); 
          window.dispatchEvent(evt);
        }
      }, 500); // trigger the resize event to resize Highcharts container
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
    },
    lang(state, lang) {
      console.debug("Define user language", lang);
      state.user.setLocale(lang);
      state.lang = lang;
    },
    storeCandidatePage(state, router) {
      state.previousPageCandidate = computePage(router);
    },
    validateCandidatePage(state) {
      if (state.previousPageCandidate) {
        state.previousPage.push(state.previousPageCandidate);
      }
      state.previousPageCandidate = null;
    }, 
    storeReturnPage(state, router) {
      state.previousPage.push(computePage(router));
    },
    goBack(state) {
      state.previousPage.pop();
    }
  },
  actions: {

  },
  modules: {
  }

});

export default store;
