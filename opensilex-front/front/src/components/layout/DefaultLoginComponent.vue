<template>
      <div class="fullmodal auth-wrapper" v-if="!isLoggedIn" >

<div class="container-fluid h-100">
  <div class="row h-100 bg-white">

        <!-- Background image -->
        <div class="col-xl-8 col-lg-6 col-md-5 p-0 d-none d-md-block">

          <div id="loginImagesCarousel" class="carousel slide carousel-fade" data-bs-ride="carousel">
            <div class="carousel-inner">
              <div
                  v-for="imagePath in carouselImagesPaths"
                  :class="carouselImagesPaths.indexOf(imagePath) == 0 ? 'carousel-item active' : 'carousel-item'">
                <img :src="opensilex.getResourceURI(imagePath)" class="d-block w-100 h-100">
              </div>
            </div>

            <template v-if="carouselImagesPaths.length > 1">
              <button
                  class="carousel-control-prev" type="button" data-bs-target="#loginImagesCarousel"
                  data-bs-slide="prev">
                <span class="carousel-control-prev-icon" aria-hidden="true"></span>
              </button>
              <button class="carousel-control-next" type="button" data-bs-target="#loginImagesCarousel"
                      data-bs-slide="next">
                <span class="carousel-control-next-icon" aria-hidden="true"></span>
              </button>
            </template>
          </div>
        </div>

        <div class="col-xl-4 col-lg-6 col-md-7 my-auto p-0">
          <!-- Language Selector -->
          <div class="languagesDropdown">
            <button class="btn dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
            <i class="bi bi-globe"></i>
              {{ t("component.common.language." + locale) }}
            <i class="bi bi-chevron-down"></i>
            </button>
            <ul class="dropdown-menu">
              <li
                v-for="lang in availableLocales"
                :key="lang"
                @click.prevent="setLanguage(lang)"
              >
                <button class="dropdown-item" @click.prevent="setLanguage(lang)">
                  {{ t("component.common.language." + lang) }}
                </button>
              </li>
            </ul>
          </div>

          <div class="authentication-form mx-auto">
            <!-- Logo -->
            <div class="logo-centered d-flex justify-content-center align-items-center">
              <slot name="loginLogo">
                <img
                  v-bind:src="opensilex.getResourceURI(props.loginLogoPath)"
                  alt="loginLogo"
                />
              </slot>
            </div>

            <!-- Guest Connection -->
            <span v-if="connectAsGuest">
              <div class="trademark">
                <slot name="guestLogin">
                  <p>
                    {{ t('component.login.info-guest') }}
                  </p>
                </slot>
                <button
                  class="btn btn-success greenThemeColor"
                  @click="onLoginAsGuest"
                >
                  {{ t('component.login.loginAsGuest') }}
                </button>
              </div>
              <br>
            </span>
              <form @submit.prevent="onLogin" class="fullmodal-form">

              <!-- Email -->
              <div class="mb-3 input-group">
                <span class="input-group-text">
                  <i class="bi bi-person"></i>
                </span>
                <input
                  id="email"
                  type="text"
                  v-model="form.email"
                  class="form-control"
                  required
                  :placeholder="t('component.login.input.email')"
                />
              </div>

              <!-- Password -->

              <div class="mb-3 input-group">
                <span class="input-group-text">
                  <i class="bi bi-lock"></i>
                </span>
                <input
                  id="password"
                  type="password"
                  v-model="form.password"
                  class="form-control"
                  required
                  :placeholder="t('component.login.input.password')"
                />
              </div>


              <!-- Forgot Password Link -->
              <router-link v-if="isResetPassword()" to="/forgot-password">
                <span>{{ t("component.forgot-password.title") }}</span>
              </router-link>

              <!-- Login Button -->
              <div class="sign-btn text-center">
                <button type="submit" class="btn btn-success greenThemeColor">
                  {{ t("component.login.button.login") }}
                </button>
              </div>
            </form>


            <div class="trademark">
              <slot name="loginFooter">
                <p>
                  {{ t("component.login.copyright.3", { version: versionInfo.version }) }}
                  <br />
                  {{ t("component.login.copyright.4", { version: versionInfo.version }) }}
                </p>
              </slot>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, nextTick, onMounted, ref} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {User} from "@/models/User";

import {FrontConfigDTO} from "@/lib";
import {useI18n} from "vue-i18n";
import {Carousel, Dropdown} from "bootstrap";
import {useStore} from "vuex";
import {VersionInfoDTO} from "opensilex-core/lib";
import {AuthenticationService, TokenGetDTO} from "opensilex-security/lib";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";

//#region public
const props = withDefaults(defineProps<{
  loginLogoPath?: string
  carouselImagesPaths?: Array<string>
}>(), {
  loginLogoPath: 'images/logo-opensilex.png',
  carouselImagesPaths: () => ['images/lac.jpg', 'images/vitioeno.jpg', 'images/LBE_Reacteur_de_laboratoire.jpg', 'images/phis-login-bg.jpg', 'images/opensilex-login-bg.png']
})
//#endregion

//#region Private

//#region Plugins and services
const opensilex= inject<OpenSilexVuePlugin>("$opensilex");
if (!opensilex) {
  throw new Error("L'instance $opensilex est introuvable ...");
}
const store = useStore();
const { t, locale, availableLocales } = useI18n();
//#endregion

//#region Data and computed
const form = ref({
  email: "",
  password: ""
});

const versionInfo = ref<VersionInfoDTO>({});

const isLoggedIn = computed(() => store.state.user.loggedIn);
const language = ref();

/**
* Ability to be logged as guest
*/
const connectAsGuest = computed(() => {
  const config: FrontConfigDTO = opensilex.getConfig();
  return config.connectAsGuest === true;
});
//#endregion

//#region Hooks
onMounted(() => {

  const bootstrapScript = document.createElement("script");
  bootstrapScript.src =
    "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js";
  bootstrapScript.onload = () => {
  };
  document.head.appendChild(bootstrapScript);

  versionInfo.value = opensilex.versionInfo;

  nextTick(() => {
    // Initialisation du carrousel (OK)
    const carouselElement = document.querySelector("#loginImagesCarousel");
    if (carouselElement) {
      new Carousel(carouselElement, {
        interval: 4000,
        ride: "carousel",
      });
    } else {
      console.warn("⚠️ Carrousel non trouvé !");
    }

    // Initialisation du dropdown
    const dropdownElement = document.querySelector(".dropdown-toggle");
    if (dropdownElement) {

      const dropdownInstance = new Dropdown(dropdownElement);
      dropdownElement.addEventListener("click", (event) => {
        event.preventDefault();
        dropdownInstance.toggle();
      });
    } else {
      console.warn("⚠️ Dropdown non trouvé !");
    }
  });
});
//#endregion

//#region Event handlers
// connexion principale
async function onLogin() {
  opensilex.showLoader();

  try {
    const authService = opensilex.getService<AuthenticationService>(
      "opensilex-security.AuthenticationService"
    );

    const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
      await authService.authenticate({
        identifier: form.value.email,
        password: form.value.password
      });

    const user = User.fromToken(response.response.result.token);
    opensilex.setCookieValue(user);
    store.commit("login", user);
    store.commit("refresh");

  } catch (error: any) {
    if (error.status === 403) {
      opensilex.showErrorToast(t("component.login.errors.invalid-credentials", error));
    } else {
      opensilex.showErrorToast(error);
    }
  } finally {
    opensilex.hideLoader();
  }
}

function onLoginAsGuest() {
  form.value.email = "guest@opensilex.org";
  form.value.password = "guest";
  console.log("OnLoginAsGuest - this.form", form.value)
  login().then(() => {
    form.value.email = "";
    form.value.password = "";
  });
}

function setLanguage(lang: string) {
  locale.value = lang;
  store.commit("lang", lang);
}
//#endregion

//#region methods
// Définition login (call by onLoginAsGuest)
async function login() {
  opensilex.showLoader();
  try {
    const authService = opensilex.getService<AuthenticationService>(
      "opensilex-security.AuthenticationService"
    );
    const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
      await authService.authenticate({
        identifier: form.value.email,
        password: form.value.password,
      });

    const user = opensilex.fromToken(response.response.result.token);
    opensilex.setCookieValue(user);

    store.commit("login", user);
    store.commit("refresh");
  } catch (error: any) {
    if (error.status === 403) {
      opensilex.errorHandler(error,  t("component.login.errors.invalid-credentials"));
    } else {
      opensilex.errorHandler(error);
    }
  } finally {
    opensilex.hideLoader();
  }
}

function isResetPassword() {
  const config = opensilex.getConfig();
  return config.activateResetPassword;
}
//#endregion

//#endregion
</script>

<style scoped lang="scss">
//for css see auth.css in the theme

</style>