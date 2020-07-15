/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />
/// <reference path="../../../opensilex-phis/front/types/opensilex-phis.d.ts" />
/**
 * CHANGE THIS VARIABLE IF NEEDED TO CHANGE API ENDPOINT
 */
const DEV_BASE_API_PATH = "http://localhost:8666/rest";

import "reflect-metadata"

// Allow access to global "document" variable
declare var document: any;

// Import Vue as a global window variable
import Vue, { Component } from 'vue'
declare var window: any;
window.Vue = Vue;

Vue.config.productionTip = false;

// Import and assignation to enable auto rebuild on ws library change
import * as LATEST_UPDATE from "./opensilex.dev";
Vue.prototype.LATEST_UPDATE = LATEST_UPDATE.default

import AsyncComputed from 'vue-async-computed'
Vue.use(AsyncComputed)

let urlParams = new URLSearchParams(window.location.search);

// Define if script in debug mode
let isDebug = false;
let isDevMode = false;
if (window["webpackHotUpdate"]) {
  isDevMode = true;
  isDebug = true;
} else {
  isDebug = urlParams.has("debug");
}

console.debug("URL parameters", urlParams);

// Initialise logger
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
  let splitURI = window.location.href.split("/");
  baseApi = splitURI[0] + "//" + splitURI[2] + "/rest"
}

console.debug("Base API URI:", baseApi);

// Setup store imports
import store from './models/Store'

// Local imports
console.debug("Import local files...");
import App from './App.vue'
import { FrontConfigDTO, VueJsService, ThemeConfigDTO, FontConfigDTO } from './lib'
import HttpResponse, { OpenSilexResponse } from './lib/HttpResponse'
import { User } from './models/User'
import { ModuleComponentDefinition } from './models/ModuleComponentDefinition'
import OpenSilexVuePlugin from './models/OpenSilexVuePlugin'
console.debug("Local file imports done !");

// Initialize cookie management library
console.debug("Initialize Cookie plugin...");
import VueCookies from 'vue-cookies'
Vue.use(VueCookies);
console.debug("Cookie plugin initialized !");

// Initialise bootstrap
console.debug("Initialize Bootstrap plugin...");
import BootstrapVue from 'bootstrap-vue'
Vue.use(BootstrapVue);
console.debug("Bootstrap plugin initialized !");

//Initialise DatePicker 
//https://www.npmjs.com/package/vuejs-datepicker
import Datepicker from 'vuejs-datepicker';

Vue.component('datePicker', Datepicker);

// Initialise font awesome
console.debug("Initialize FontAwesomeIcon plugin...");
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { library } from '@fortawesome/fontawesome-svg-core'
import { fas } from '@fortawesome/free-solid-svg-icons'
library.add(fas);
Vue.component('font-awesome-icon', FontAwesomeIcon)
console.debug("FontAwesomeIcon plugin initialized !");

// Initialize Vue wizard form
import VueFormWizard from 'vue-form-wizard'
import 'vue-form-wizard/dist/vue-form-wizard.min.css'
Vue.use(VueFormWizard)
console.debug("VueFormWizard plugin initialized !");

// Initialize Vue Tabulator
require('tabulator-tables');
import VueTabulator from 'vue-tabulator';
Vue.use(VueTabulator);
console.debug("VueTabulator plugin initialized !");

// Initialize Vue Country flag
import CountryFlag from 'vue-country-flag'
Vue.component('country-flag', CountryFlag);

// Initialize Tree selector
import Treeselect from '@riophae/vue-treeselect'
Vue.component("treeselect", Treeselect);

// Initialize i18n
import VueI18n from 'vue-i18n'
import en from './lang/message-en.json';
import fr from './lang/message-fr.json';

let lang = navigator.language;

if (urlParams.has('lang')) {
  lang = urlParams.get("lang");
}

if (lang && lang.length > 2) {
  lang = lang.substr(0, 2);
}

console.debug("Detected language", lang);
let i18nOptions = {
  fallbackLocale: 'en',
  locale: lang,
  silentTranslationWarn: !isDebug,
  silentFallbackWarn: !isDebug,
  messages: {
    "en": {},
    "fr": {}
  }
}
const i18n = new VueI18n(i18nOptions);

//Initialise validation 
//https://logaretm.github.io/vee-validate/
import { ValidationProvider, ValidationObserver } from 'vee-validate';
Vue.component('ValidationProvider', ValidationProvider);
Vue.component('ValidationObserver', ValidationObserver);

import { configure, extend } from 'vee-validate';
import validationMessagesEN from 'vee-validate/dist/locale/en.json';
import validationMessagesFR from 'vee-validate/dist/locale/fr.json';
import * as rules from 'vee-validate/dist/rules';
import { email } from 'vee-validate/dist/rules';

for (let [rule, validation] of Object.entries(rules)) {
  extend(rule, {
    ...validation
  });
}

extend("url", (value) => {
  try {
    new URL(value);
    return true;
  } catch (error) {
    return false;
  }
});

extend("emailOrUrl", (value) => {
  try {
    new URL(value);
    return true;
  } catch (error) {
    return email.validate(value);
  }
});


let validationTranslations = {
  "validations": validationMessagesEN.messages
}

i18n.mergeLocaleMessage("en", validationTranslations);
i18n.mergeLocaleMessage("en", en);

validationTranslations = {
  "validations": validationMessagesFR.messages
}
i18n.mergeLocaleMessage("fr", validationTranslations);
i18n.mergeLocaleMessage("fr", fr);

configure({
  classes: {
    valid: 'is-valid',
    invalid: 'is-invalid'
  },
  defaultMessage: (_, values) => {
    return "" + i18n.t(`validations.${values._rule_}`, values)
  }
});

// Initialize date picker
import DatePicker from 'vue-airbnb-style-datepicker';

const datepickerOptions = {
  monthNames: [
    i18n.t('component.common.datePicker.monthNames.january'),
    i18n.t('component.common.datePicker.monthNames.february'),
    i18n.t('component.common.datePicker.monthNames.march'),
    i18n.t('component.common.datePicker.monthNames.april'),
    i18n.t('component.common.datePicker.monthNames.may'),
    i18n.t('component.common.datePicker.monthNames.june'),
    i18n.t('component.common.datePicker.monthNames.july'),
    i18n.t('component.common.datePicker.monthNames.august'),
    i18n.t('component.common.datePicker.monthNames.september'),
    i18n.t('component.common.datePicker.monthNames.october'),
    i18n.t('component.common.datePicker.monthNames.november'),
    i18n.t('component.common.datePicker.monthNames.december')
  ],
  days: [
    i18n.t('component.common.datePicker.dayNames.monday'),
    i18n.t('component.common.datePicker.dayNames.tuesday'),
    i18n.t('component.common.datePicker.dayNames.wednesday'),
    i18n.t('component.common.datePicker.dayNames.thursday'),
    i18n.t('component.common.datePicker.dayNames.friday'),
    i18n.t('component.common.datePicker.dayNames.saturday'),
    i18n.t('component.common.datePicker.dayNames.sunday')
  ],
  daysShort: [
    i18n.t('component.common.datePicker.dayShortNames.mon'),
    i18n.t('component.common.datePicker.dayShortNames.tue'),
    i18n.t('component.common.datePicker.dayShortNames.wed'),
    i18n.t('component.common.datePicker.dayShortNames.thu'),
    i18n.t('component.common.datePicker.dayShortNames.fri'),
    i18n.t('component.common.datePicker.dayShortNames.sat'),
    i18n.t('component.common.datePicker.dayShortNames.sun')
  ],
  texts: {
    apply: i18n.t('component.common.datePicker.texts.apply'),
    cancel: i18n.t('component.common.datePicker.texts.cancel')
  }
};

Vue.use(DatePicker, datepickerOptions);

// Enable Vue front plugin manager for OpenSilex API
console.debug("Enable OpenSilex plugin...");
let $opensilex = new OpenSilexVuePlugin(baseApi, store, i18n);
$opensilex.setCookieSuffix(baseApi);
Vue.use($opensilex);
console.debug("OpenSilex plugin enabled !");

// Define global error manager
console.debug("Define global error manager");
const manageError = function manageError(error) {
  console.error(error);
  document.getElementById('opensilex-error-loading').style.visibility = 'visible';
}

// Load tree component
console.debug("Load tree component...");
import SlVueTree from 'sl-vue-tree';
Vue.component("sl-vue-tree", SlVueTree);

// Load default components
console.debug("Load default components...");
import components from './components';
for (let componentName in components) {
  console.debug("Load default component", componentName);
  let component = components[componentName];
  Vue.component(componentName, component);
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
              resolve();
            };
            link.onerror = reject;
            link.setAttribute("href", cssURI);
            document.getElementsByTagName("head")[0].appendChild(link);
          } else {
            resolve();
          }
        })
        .catch(reject)
    } else {
      console.debug("No theme defined !");
      resolve();
    }
  })
}

$opensilex.loadModules([
  "opensilex-security",
  "opensilex-core",
]).then(() => {
  $opensilex.initAsyncComponents(components).then(() => {
    console.debug("Default components loaded !");

    // Get OpenSilex configuration
    console.debug("Start loading configuration...");
    const vueJsService = $opensilex.getService<VueJsService>("VueJsService");
    vueJsService.getConfig()
      .then(function (configResponse) {
        const config: FrontConfigDTO = configResponse.response.result;
        $opensilex.setConfig(config);

        let themePromise: Promise<any> = loadTheme(vueJsService, config);

        themePromise.then(() => {
          store.commit("setConfig", config);
          console.debug("Configuration loaded", config);

          // Load only necessary component if application is embed in an iframe
          let embed = urlParams.has('embed');

          if (embed) {
            console.debug("Application is embed");
          } else {
            console.debug("Application is not embed");
          }

          // Define user
          console.debug("Define current user...");
          let user: User | undefined = undefined;
          if (urlParams.has("token")) {
            let token = urlParams.get("token");
            console.debug("Try to load user from URL token...");
            if (token != null) {
              user = User.fromToken(token);
              $opensilex.setCookieValue(user);
              console.debug("User sucessfully loaded from URL token !");
            }
          }

          if (user == undefined) {
            console.debug("Try to load user from cookie...");
            user = $opensilex.loadUserFromCookie();
            console.debug("User sucessfully loaded from cookie !");
          }

          if (!user.isLoggedIn()) {
            console.debug("User is ANONYMOUS !");
            i18n.locale = lang;
          } else {
            console.debug("User is:", user.getEmail());
            i18n.locale = user.getLocale() || lang;
          }
          // Init user
          console.debug("Initialize global user");
          store.commit("login", user);

          // Init routing
          console.debug("Initialize routing");
          let router = store.state.openSilexRouter.getRouter();

          // Initalise main layout components from configuration
          console.debug("Define initial modules to load...");
          let modulesToLoad: ModuleComponentDefinition[] = [
            ModuleComponentDefinition.fromString(config.homeComponent),
            ModuleComponentDefinition.fromString(config.notFoundComponent)
          ];

          if (!embed) {
            console.debug("Application is not embed");
            modulesToLoad = modulesToLoad.concat([
              ModuleComponentDefinition.fromString(config.footerComponent),
              ModuleComponentDefinition.fromString(config.headerComponent),
              ModuleComponentDefinition.fromString(config.loginComponent),
              ModuleComponentDefinition.fromString(config.menuComponent)
            ]);
          } else {
            console.debug("Application is embed");
          }

          $opensilex.loadComponentModules(modulesToLoad)
            .then(() => {
              // Initialize main application rendering
              console.debug("Initialize main application rendering");
              new Vue({
                router,
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
                i18n
              }).$mount('#app').$nextTick(() => {
                // Hide loader
                console.debug("Hide application init loader");
                document.getElementById('opensilex-loader').style.visibility = 'hidden';
              });
            })
        })
      })

  })
})