/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />
/**
 * CHANGE THIS VARIABLE IF NEEDED TO CHANGE API ENDPOINT
 */
const DEV_BASE_API_PATH = "http://localhost:8666/rest";

// import App from './App.vue'
import { createApp } from "vue";
import { createI18n } from 'vue-i18n';
import en from './lang/message-en.json';
import fr from './lang/message-fr.json';

console.log("Début du main.ts")

console.log("mount app")
const i18n = createI18n({
  locale: 'fr',
  messages: {
    en,
    fr
  }
});
// const app = createApp(App)
// .use(i18n);

// app.mount('#app');

import "reflect-metadata"

// Allow access to global "document" variable
declare var document: any;

// Import Vue as a global window variable
// import Vue from 'vue';
// import VueMatomo from 'vue-matomo';
declare var window: any;
// window.Vue = Vue;

// Vue.config.productionTip = false;

// Import and assignation to enable auto rebuild on ws library change
//@todo je sais pas ce que c'est
// import * as LATEST_UPDATE from "./opensilex.dev";
// Vue.prototype.LATEST_UPDATE = LATEST_UPDATE.default

// import AsyncComputed from 'vue-async-computed'
// Vue.use(AsyncComputed)

let urlParams = new URLSearchParams(window.location.search);

// Define if script in debug mode
let isDebug = true;
let isDevMode = true;

console.debug("URL parameters", urlParams);

// Initialize logger
console.log = console.log || function () { };
console.warn = console.warn || console.log;
console.error = console.error || console.log;
if (isDebug) {
  console.debug = console.debug || console.log;
} else {
  console.debug = function () { };
}
console.debug("Logger initialized in debug mode");

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

console.debug("Base API URI:", baseApi);

// Setup store imports
import store from './models/Store'

// Local imports
console.debug("Import local files...");
import {FrontConfigDTO, VueJsService, ThemeConfigDTO, FontConfigDTO, UserFrontConfigDTO} from './lib'
import HttpResponse, { OpenSilexResponse } from './lib/HttpResponse'
import { User } from './models/User'
import { ModuleComponentDefinition } from './models/ModuleComponentDefinition'
import OpenSilexVuePlugin from './models/OpenSilexVuePlugin'
console.debug("Local file imports done !");



// console.debug("Enable OpenSilex plugin...");
// const $opensilex = new OpenSilexVuePlugin(baseApi, store, null);

const manageError = function manageError(error) {
  console.error(error);
}


// Load default components
console.debug("Load default components...");
import components from './components';
// import { Router } from 'vue-router';
// @ts-ignore
import { AuthenticationService } from "opensilex-security/index";
import App from './App.vue'

const app = createApp(App);
const $opensilex = new OpenSilexVuePlugin(baseApi, store, null);
app.config.globalProperties.$opensilex = $opensilex;
app.use(i18n)
app.use($opensilex);
app.use(store);

(store as any).$opensilex = $opensilex;



for (let componentName in components) {
  console.log("Load default components : ", componentName);
  let component = components[componentName];
  //@todo est-ce qu'on a vraiment besoin ? pas de manière plus propre de faire ? sinon trouver le moyen en vue 3 
  // Vue.component(componentName, component);
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
      console.debug("Load defined theme configuration...", config.themeModule, config.themeName);
      vueJsService.getThemeConfig(config.themeModule, config.themeName)
        .then((http: HttpResponse<OpenSilexResponse<ThemeConfigDTO>>) => {
          console.debug("Theme configuration loaded !", config.themeModule, config.themeName);
          const themeConfig: ThemeConfigDTO = http.response.result;

          $opensilex.setThemeConfig(themeConfig);
          loadFonts(vueJsService, themeConfig.fonts);

          if (themeConfig.hasStyle) {
            console.debug("Load CSS theme style...");
            const cssURI = baseApi + "/vuejs/theme/" + encodeURIComponent(config.themeModule) + "/" + encodeURIComponent(config.themeName) + "/style.css";
            var link = document.createElement('link');
            link.setAttribute("rel", "stylesheet");
            link.setAttribute("type", "text/css");
            link.onload = function () {
              console.debug("CSS theme style loaded !");
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
      console.debug("No theme defined !");
      resolve(true);
    }
  })
}


$opensilex.loadModules([
  "opensilex-security",
  "opensilex-core",
  "opensilex-dataverse"
]).then(() => {
  console.log("----------- loading")
  
  $opensilex.initAsyncComponents(components).then(() => {
    console.debug("Default components loaded !");


    // Get OpenSilex configuration
    console.debug("Start loading configuration...");
    let loadConfigFromOSVuePlugin = $opensilex.getConfig()
    // loadConfigFromOSVuePlugin;
    console.log(loadConfigFromOSVuePlugin)
    const vueJsService = $opensilex.getService<VueJsService>("VueJsService");
    vueJsService.getConfig().then((configResponse) => {
      console.log("can we get a config PLEASE", configResponse)
      const config: FrontConfigDTO = configResponse.response.result;
      $opensilex.setConfig(config);

      store.commit("setConfig", config);
      console.debug("Configuration loaded", config);

      // Define user
      console.debug("Define current user...");
      let user: User | undefined = undefined;
      if (urlParams.has("token")) {
        let token = urlParams.get("token");
        console.debug("Try to load user from URL token...");
        if (token != null) {
          user = User.fromToken(token);
          $opensilex.setCookieValue(user);
          console.debug("User successfully loaded from URL token !");
        }
      }

      if (user == undefined) {
        console.debug("Try to load user from cookie...");
        user = $opensilex.loadUserFromCookie();
        console.debug("User successfully loaded from cookie !");
      }

      // Set language
      if (!user.isLoggedIn()) {
        console.debug("User is ANONYMOUS !");
        // i18n.locale = lang;
      } else {
        console.debug("User is:", user.getEmail());
        // i18n.locale = user.getLocale() || lang;
      }

      // Init user in store
      console.debug("Initialize global user");
      store.commit("login", user);


      // Load user-specific configuration
      console.debug("Start loading user-specific configuration...");
      vueJsService.getUserConfig()
        .then(function (userConfigResponse) {
          const userConfig: UserFrontConfigDTO = userConfigResponse.response.result;

          store.commit("setUserConfig", userConfig);
          console.debug("User-specific configuration loaded", userConfig);

          let baseURL = window.location.href.split(/[?#]/)[0];

          // If user is anonymous in the API but logged in according to the cookie, we must force the logout
          if (userConfig.userIsAnonymous && user.isLoggedIn()) {
            console.debug("User should be anonymous, force logout");
            store.commit("logout");
            window.location = baseURL;
          }

          const authService = $opensilex.getService<AuthenticationService>("AuthenticationService");
          console.log("MAIN - bageURL", baseURL)
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
                  console.debug("Application is embed");
                } else {
                  console.debug("Application is not embed");
                }

                // Init routing
                console.debug("Initialize routing");
                store.commit("resetRouter");
                // let router: Router = store.state.openSilexRouter.getRouter();

                // Initialise main layout components from configuration
                console.debug("Define initial modules to load...");
                let modulesToLoad: ModuleComponentDefinition[] = [
                  ModuleComponentDefinition.fromString(config.homeComponent),
                  ModuleComponentDefinition.fromString(config.notFoundComponent)
                ];

                if (!embed) {
                  console.log("Application is not embed");
                  // modulesToLoad = modulesToLoad.concat([
                  //   ModuleComponentDefinition.fromString(config.footerComponent),
                  //   ModuleComponentDefinition.fromString(config.headerComponent),
                    // ModuleComponentDefinition.fromString(config.loginComponent)
                  //   ModuleComponentDefinition.fromString(config.menuComponent)
                  // ]);
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
                  $opensilex.loadComponentModules(modulesToLoad)
                ]).then(() => {
                  // Initialize main application rendering
                  console.debug("Initialize main application rendering");
                  let vueOptions: any = {
                    // router,
                    store,
                    render: h => h(App, {
                      props: {
                        embed: embed,
                        footerComponent: config.footerComponent,
                        headerComponent: config.headerComponent,
                        loginComponent: config.loginComponent,
                        menuComponent: config.menuComponent
                      }
                    },
                    ),
                    // i18n
                    undefined
                  };

                  // Load matomo
                  // if (config.matomo.serverUrl) {
                  //   console.debug(`Configuring Matomo with server URL ${config.matomo.serverUrl}`);
                  //   // See https://github.com/AmazingDreams/vue-matomo for configuration
                  //   Vue.use(VueMatomo, {
                  //     host: config.matomo.serverUrl,
                  //     siteId: config.matomo.siteId,
                  //     trackerFileName: 'matomo',
                  //     router,
                  //     enableLinkTracking: true,
                  //     requireConsent: false,
                  //     trackInitialView: true,
                  //     disableCookies: false,
                  //     requireCookieConsent: false,
                  //     enableHeartBeatTimer: false,
                  //     heartBeatTimerInterval: 15,
                  //     debug: false,
                  //     userId: undefined,
                  //     cookieDomain: undefined,
                  //     domains: undefined,
                  //     preInitActions: [],
                  //     trackSiteSearch: false,
                  //     crossOrigin: undefined
                  //   });
                  // }


                  // new Vue(vueOptions).$mount('#app').$nextTick(() => {
                  //   // Hide loader
                  //   console.debug("Hide application init loader");
                  //   document.getElementById('opensilex-loader').style.visibility = 'hidden';
                  // });

                  app.mount('#app');


                }).catch(manageError);
              }).catch(manageError);
          }
      }).catch(manageError);
    }).catch(manageError);
  }).catch(manageError);
}).catch(manageError);
