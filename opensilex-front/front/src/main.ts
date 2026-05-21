/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />
/**
 * CHANGE THIS VARIABLE IF NEEDED TO CHANGE API ENDPOINT
 */
const DEV_BASE_API_PATH = "http://localhost:8666/rest";


// Import global de Bootstrap (CSS + JS)
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap';

import { createApp, ref, reactive, computed } from "vue";
import { createI18n } from 'vue-i18n';
import en from './lang/message-en.json';
import fr from './lang/message-fr.json';
import "bootstrap-icons/font/bootstrap-icons.css";
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { library } from '@fortawesome/fontawesome-svg-core'
import { fas } from '@fortawesome/free-solid-svg-icons'
import ToastContainer from './components/common/toastContainer.vue';
import { create, NButton, NDataTable, NDropdown, NTree, NList, NListItem, NInput, NSpace, NTag, DataTableRowKey, NDrawer, NDrawerContent, NForm, NFormItem, NSwitch, NCheckbox, NCollapse, NCollapseItem, NDivider } from 'naive-ui';
import vue3Tour from 'vue3-tour';
import 'vue3-tour/dist/vue3-tour.css';

// Ajoute toutes les icônes solides à la bibliothèque
library.add(fas);
const iconIDs = Array.from(new Set(Object.values(fas).map(def => def.iconName))).map(iconName => ({
    id: "fa#" + iconName,
    iconName
}));

let lang = navigator.language;

if (lang && lang.length > 2) {
  lang = lang.substring(0, 2);
}

const naive = create({
  components: [NButton, NDataTable, NDropdown, NTree]
});

const i18n = createI18n({
  fallbackLocale: 'en',
  locale: lang,

  messages: {
    en,
    fr
  },
  dateTimeFormats: {
    "en": {
      short: {
        year: 'numeric', month: '2-digit', day: '2-digit'
      }
    },
    "fr": {
      short: {
        day: '2-digit', month: '2-digit', year: 'numeric'
      }
    }
  },
  numberFormats: {
    "en": {
      decimal: {
        style: 'decimal',
      },
    },
    "fr": {
      decimal: {
        style: 'decimal'
      },
    }
  }

});

import "reflect-metadata"

// Allow access to global "document" variable
declare var document: any;

// Import Vue as a global window variable 
// import VueMatomo from 'vue-matomo';
declare var window: any;
// Attach Vue APIs to window
window.Vue = { createApp, ref, reactive, computed };

// Vue.config.productionTip = false;

// Import and assignation to enable auto rebuild on ws library change
// @todo je sais pas ce que c'est
// import * as LATEST_UPDATE from "./opensilex.dev";
// Vue.prototype.LATEST_UPDATE = LATEST_UPDATE.default

// import AsyncComputed from 'vue-async-computed'
// Vue.use(AsyncComputed)

let urlParams = new URLSearchParams(window.location.search);

// Define if script in debug mode
let isDebug = false;
let isDevMode = false;

if (import.meta.env.DEV) {
  isDevMode = true;
  isDebug = true;
} else {
  isDebug = urlParams.has("debug");
}
// console.debug("URL parameters", urlParams);

// Initialize logger
console.log = console.log || function () { };
console.warn = console.warn || console.log;
console.error = console.error || console.log;
if (isDebug) {
  console.debug = console.debug || console.log;
} else {
  console.debug = function () { };
}

// Initialize service API container
let baseApi = DEV_BASE_API_PATH;
if (isDevMode) {
  console.warn(
    'Vue is running in development mode, with base API set by default to ' + DEV_BASE_API_PATH + '\n' +
    'If you start your webservices server with another host or port configuration,\n' +
    'please edit opensilex-front/front/src/main.ts and update DEV_BASE_API_PATH constant'
  );
} else {
  let splitURI = window.location.href.split("/app");
  baseApi = splitURI[0] + "/rest"
}

// Setup store imports
import store from './models/Store'

// Local imports
// console.debug("Import local files...");
import { FrontConfigDTO, VueJsService, ThemeConfigDTO, FontConfigDTO, UserFrontConfigDTO } from './lib'
import HttpResponse, { OpenSilexResponse } from './lib/HttpResponse'
import { User } from './models/User'
import { ModuleComponentDefinition } from './models/ModuleComponentDefinition'
import OpenSilexVuePlugin from './models/OpenSilexVuePlugin'

const manageError = function manageError(error) {
  console.error(error);
}


// Load default components
// console.debug("Load default components...");
import components from './components';
import { Router } from 'vue-router';
// @ts-ignore
import { AuthenticationService } from "opensilex-security/index";
import App from './App.vue';
import HighchartsVue from "highcharts-vue";
import Highcharts from 'highcharts';


const app = createApp(App);

const $opensilex = new OpenSilexVuePlugin(baseApi, store, null);
$opensilex.setIconIDs(iconIDs)

// Fournit l'instance pour injection dans Vue 3
app.provide("$opensilex", $opensilex);

// Ajout aux propriétés globales (utile pour l'option API)
app.config.globalProperties.$opensilex = $opensilex;

app.use(i18n)
app.use($opensilex);
app.use(store);
app.use(naive);
app.use(HighchartsVue);
app.use(vue3Tour);
app.component('font-awesome-icon', FontAwesomeIcon);


(store as any).$opensilex = $opensilex;


for (let componentName in components) {
  let component = components[componentName];
  app.component(componentName, component);
  $opensilex.loadComponentTranslations(component);
}



function loadFonts(vueJsService: VueJsService, fonts: Array<FontConfigDTO>) {

  for (let i in fonts) {
    let font: FontConfigDTO = fonts[i];

    console.debug("Loading font:", font.family);

    let fontStyle = document.createElement('style');
    let fontFace = [
      "@font-face {",
      "font-family: '" + font.family + "';",
      "font-style: '" + font.style + "';",
      "font-weight: '" + font.weight + "';",
      "src: url('" + $opensilex.getResourceURI(font.url) + "');"
    ];

    let fontCount = Object.keys(font.src).length;
    if (fontCount > 0) {
      fontFace.push("src: local( '" + font.family + "'),");

      let i = 0;
      for (let typeFormat in font.src) {
        i++;
        let formatUrl = "url('" + $opensilex.getResourceURI(font.src[typeFormat]) + "') format('" + typeFormat + "')";
        if (i == fontCount) {
          formatUrl += ";"
        } else {
          formatUrl += ","
        }

        fontFace.push(formatUrl);
      }
    }

    fontFace.push("}");

    fontStyle.appendChild(document.createTextNode(fontFace.join("\n")));

    document.head.appendChild(fontStyle);
  }
}

function loadTheme(vueJsService: VueJsService, config: FrontConfigDTO) {
  return new Promise((resolve, reject) => {
    if (config.themeModule && config.themeName) {
      // console.debug("Load defined theme configuration...", config.themeModule, config.themeName);
      vueJsService.getThemeConfig(config.themeModule, config.themeName)
        .then((http: HttpResponse<OpenSilexResponse<ThemeConfigDTO>>) => {
          // console.debug("Theme configuration loaded !", config.themeModule, config.themeName);
          const themeConfig: ThemeConfigDTO = http.response.result;

          $opensilex.setThemeConfig(themeConfig);
          loadFonts(vueJsService, themeConfig.fonts);

          if (themeConfig.hasStyle) {
            // console.debug("Load CSS theme style...");
            const cssURI = baseApi + "/vuejs/theme/" + encodeURIComponent(config.themeModule) + "/" + encodeURIComponent(config.themeName) + "/style.css";
            var link = document.createElement('link');
            link.setAttribute("rel", "stylesheet");
            link.setAttribute("type", "text/css");
            link.onload = function () {
              // console.debug("CSS theme style loaded !");
              resolve(true);
            };
            link.onerror = reject;
            link.setAttribute("href", cssURI);
            document.getElementsByTagName("head")[0].appendChild(link);
          } else {
            resolve(true);
          }
        })
        .catch(reject)
    } else {
      // console.debug("No theme defined !");
      resolve(true);
    }
  })
}

$opensilex.loadModules([
  "opensilex-security",
  "opensilex-core",
  // "opensilex-dataverse"
]).then(() => {
  // Not seems mandatory in vue 3, need to test when component are loaded from other modules
  // Get OpenSilex configuration
  // console.debug("Start loading configuration...");
  let loadConfigFromOSVuePlugin = $opensilex.getConfig()
  // loadConfigFromOSVuePlugin;
  const vueJsService = $opensilex.getService<VueJsService>("VueJsService");
  vueJsService.getConfig().then((configResponse) => {
    const config: FrontConfigDTO = configResponse.response.result;
    $opensilex.setConfig(config);

    store.commit("setConfig", { config, app });
    // console.debug("Configuration loaded", config);

    // Define user
    // console.debug("Define current user...");
    let user: User | undefined = undefined;
    if (urlParams.has("token")) {
      let token = urlParams.get("token");
      // console.debug("Try to load user from URL token...");
      if (token != null) {
        user = User.fromToken(token);
        $opensilex.setCookieValue(user);
        // console.debug("User successfully loaded from URL token !");
      }
    }

    if (user == undefined) {
      user = $opensilex.loadUserFromCookie();
    }

    // Set language
    if (!user.isLoggedIn()) {
      // console.debug("User is ANONYMOUS !");
      // i18n.locale = lang;
    } else {
      // console.debug("User is:", user.getEmail());
      // i18n.locale = user.getLocale() || lang;
    }
    // Init user in store
    store.commit("login", user);


    // Load user-specific configuration
    vueJsService.getUserConfig()
      .then(function (userConfigResponse) {
        const userConfig: UserFrontConfigDTO = userConfigResponse.response.result;

        store.commit("setUserConfig", userConfig);

        let baseURL = window.location.href.split(/[?#]/)[0];

        // If user is anonymous in the API but logged in according to the cookie, we must force the logout
        if (userConfig.userIsAnonymous && user.isLoggedIn()) {
          console.debug("User should be anonymous, force logout");
          store.commit("logout");
          window.location = baseURL;
        }

        const authService = $opensilex.getService<AuthenticationService>("AuthenticationService");
        if (baseURL.endsWith("/app/openid") && urlParams.has('code')) {
          console.debug("Identify user with OpenID Connect");
          authService.authenticateOpenID(urlParams.get("code"))
            .then((http) => {
              let user = User.fromToken(http.response.result.token);
              $opensilex.setCookieValue(user);
              window.location = baseURL.slice(0, -7);
            }).catch(manageError);
        } else if (baseURL.endsWith("/app/saml") && urlParams.has("token")) {
          console.debug("Authenticate user through SAML");
          let user = User.fromToken(urlParams.get("token"));
          $opensilex.setCookieValue(user);
          window.location = baseURL.slice(0, -5);
        } else {
          let themePromise: Promise<any> = loadTheme(vueJsService, config);

          themePromise
            .then(() => {
              // Load only necessary component if application is embed in an iframe
              let embed = urlParams.has('embed');

              if (embed) {
                // console.debug("Application is embed");
              } else {
                // console.debug("Application is not embed");
              }

              // Init routing
              store.commit("resetRouter");
              let router: Router = store.state.openSilexRouter.getRouter();

              // Register router so components can access it during loading
              app.use(router);

              // Initialise main layout components from configuration
              let modulesToLoad: ModuleComponentDefinition[] = [
                ModuleComponentDefinition.fromString(config.homeComponent),
                ModuleComponentDefinition.fromString(config.notFoundComponent)
              ];

              if (!embed) {
                modulesToLoad = modulesToLoad.concat([
                  ModuleComponentDefinition.fromString(config.footerComponent),
                  ModuleComponentDefinition.fromString(config.headerComponent),
                  ModuleComponentDefinition.fromString(config.loginComponent),
                  ModuleComponentDefinition.fromString(config.menuComponent)
                ]);
              } else {
                console.log("Application is embed");
                console.debug("Application is embed");
              }

              Promise.all([
                $opensilex.loadVersionInfo(),
                $opensilex.loadFactorCategories(),
                $opensilex.loadDataTypes(),
                $opensilex.loadVariableDataTypes(),
                $opensilex.loadNameSpaces(),
                $opensilex.loadObjectTypes(),
                $opensilex.loadComponentModules(modulesToLoad),
                router.isReady()
              ]).then(() => {
                app.mount('#app');


              }).catch(manageError);
            }).catch(manageError);
        }
      }).catch(manageError);
  }).catch(manageError);
}).catch(manageError);
