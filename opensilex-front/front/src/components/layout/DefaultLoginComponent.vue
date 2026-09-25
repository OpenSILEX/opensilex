<template>
      <div class="fullmodal auth-wrapper" v-if="!isLoggedIn" >

<div class="container-fluid h-100">
  <div class="row h-100 bg-white">

        <!-- Background image -->
        <div class="col-xl-8 col-lg-6 col-md-5 p-0 d-none d-md-block">

            <!-- low opacity green layout used for phis bg image-->
            <!-- <div class="lavalite-overlay"></div> -->

          <div id="loginImagesCarousel" class="carousel slide carousel-fade" data-bs-ride="carousel">
            <div class="carousel-inner">
              <div class="carousel-item active">
                <img :src="$opensilex.getResourceURI('images/lac.jpg')"  class="d-block w-100 h-100">
              </div>
              <div class="carousel-item">
                <img :src="$opensilex.getResourceURI('images/vitioeno.jpg')" class="d-block w-100 h-100" >
              </div>
                  <div class="carousel-item">
                <img :src="$opensilex.getResourceURI('images/LBE_Reacteur_de_laboratoire.jpg')" class="d-block w-100 h-100" >
              </div>
                  <div class="carousel-item">
                <img :src="$opensilex.getResourceURI('images/phis-login-bg.jpg')" class="d-block w-100 h-100" >
              </div>
                  <div class="carousel-item">
                <img :src="$opensilex.getResourceURI('images/opensilex-login-bg.png')" class="d-block w-100 h-100" >
              </div>
            </div>

            <button class="carousel-control-prev" type="button" data-bs-target="#loginImagesCarousel" data-bs-slide="prev">
              <span class="carousel-control-prev-icon" aria-hidden="true"></span>
            </button>
            <button class="carousel-control-next" type="button" data-bs-target="#loginImagesCarousel" data-bs-slide="next">
              <span class="carousel-control-next-icon" aria-hidden="true"></span>
            </button>
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
                  v-bind:src="
                    $opensilex.getResourceURI('images/logo-opensilex.png')
                  "
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
                <!--
                  à reintroduire plus tard :
                  errors = slot en provenance de validationProvider donc pas dispo tant que probleme avec validation provider...
                <div v-if="errors.email" class="error-message alert alert-danger">
                  {{ errors.email }}
                </div> -->
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
                <!-- 
                  à reintroduire plus tard :
                  errors = slot en provenance de validationProvider donc pas dispo tant que probleme avec validation provider...
                  <div v-if="errors.password" class="error-message alert alert-danger">
                  {{ errors.password }}
                </div> -->
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

<script lang="ts">
import {computed, defineComponent, inject, nextTick, onMounted, ref} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {User} from "../../models/User";
import type {AuthenticationService, TokenGetDTO} from "opensilex-security/index";
import type {OpenSilexResponse} from "opensilex-security/HttpResponse";
import HttpResponse from "opensilex-security/HttpResponse";

import {FrontConfigDTO} from "../../lib";
import {VersionInfoDTO} from "opensilex-core/index";
import {useI18n} from "vue-i18n";
import {Carousel, Dropdown} from "bootstrap";
import {useStore} from "vuex";
import {useRoute, useRouter} from 'vue-router'

export default defineComponent({
  name: 'defaultLoginComponent',
  props: {
    $opensilex: OpenSilexVuePlugin
  },
  setup() {
    // injection des dépendances
    const $opensilex= inject<OpenSilexVuePlugin>("$opensilex");
    const router = useRouter();
    const store = useStore();
    const user = computed(() => store.state.user);
    const isLoggedIn = computed(() => store.state.user.loggedIn);
    const resetPasswordPath = computed(() => router.resolve("/forgot-password").href);
    const route = useRoute();

    const form = ref({
      email: "",
      password: ""
    });

    const versionInfo = ref<VersionInfoDTO>({});


    if (!$opensilex) {
      throw new Error("L'instance $opensilex est introuvable ...");
    }


    // Gestion des langues
    const language = ref();
    const { t, locale, availableLocales } = useI18n();

    /**
    * Ability to be logged as guest
    */
    const connectAsGuest = computed(() => {
      const config: FrontConfigDTO = $opensilex.getConfig();
      return config.connectAsGuest === true;
    });

    // Gestion du carrousel
    onMounted(() => {

      const bootstrapScript = document.createElement("script");
      bootstrapScript.src =
        "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js";
      bootstrapScript.onload = () => {
      };
      document.head.appendChild(bootstrapScript);

      versionInfo.value = $opensilex.versionInfo;

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

    // Définition login (call by onLoginAsGuest)
    const login = async () => {
      $opensilex.showLoader();
      try {
        const authService = $opensilex.getService<AuthenticationService>(
          "opensilex-security.AuthenticationService"
        );
        const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
          await authService.authenticate({
            identifier: form.value.email,
            password: form.value.password,
          });

        const user = $opensilex.fromToken(response.response.result.token);
        $opensilex.setCookieValue(user);

        store.commit("login", user);
        store.commit("refresh");
      } catch (error: any) {
        if (error.status === 403) {
          $opensilex.errorHandler(error,  t("component.login.errors.invalid-credentials"));
        } else {
          $opensilex.errorHandler(error);
        }
      } finally {
        $opensilex.hideLoader();
      }
    };


    // connexion principale 
    const onLogin = async () => {
      $opensilex.showLoader();

      try {
        const authService = $opensilex.getService<AuthenticationService>(
          "opensilex-security.AuthenticationService"
        );

        const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
          await authService.authenticate({
            identifier: form.value.email,
            password: form.value.password
          });

        const user = User.fromToken(response.response.result.token);
        $opensilex.setCookieValue(user);
        store.commit("login", user);
        store.commit("refresh");


      } catch (error: any) {
        if (error.status === 403) {
          console.error("onLogin - Invalid credentials", error);
          $opensilex.showErrorToast(t("component.login.errors.invalid-credentials", error));
        } else {
          $opensilex.showErrorToast(error);
        }
      } finally {
        $opensilex.hideLoader();
      }
    };


    return {
      t,
      locale,
      availableLocales,
      connectAsGuest,
      form,
      versionInfo,
      isLoggedIn,
      resetPasswordPath,
      login,
      onLogin
    };
  },

  methods: {
    setLanguage(lang: string) {
      this.$i18n.locale = lang;
      this.$store.commit("lang", lang);
    },
    onLoginAsGuest() {
      this.form.email = "guest@opensilex.org";
      this.form.password = "guest";
      console.log("OnLoginAsGuest - this.form", this.form)
      this.login().then(() => {
        this.form.email = "";
        this.form.password = "";
      });
    },
    isResetPassword() {
      const config = this.$opensilex.getConfig();
      return config.activateResetPassword;
    }
  },
});
</script>

<style scoped lang="scss">

.carousel-item {
  transition: opacity 1.5s ease-in-out !important;
}
invalidCredentials
.fullmodal {
  display: block;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  width: 100%;
  z-index: 9998; /*behind global toaster box*/
}

.authentication-form .error-message {
  top: 37px;
}

.authentication-form fieldset {
  margin-bottom: 25px;
}

.languagesDropdown {
  position: fixed;
  top: 5px;
  right: 10px;
}
// .languagesDropdown > * {
//  font-weight: bold;
//  font-size: 1.2em;
// }
</style>