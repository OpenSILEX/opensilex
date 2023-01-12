<template>
  <div class="fullmodal auth-wrapper" v-if="!user.isLoggedIn() || forceRefresh">
    <div class="container-fluid h-100">
      <div class="row flex-row h-100 bg-white">

        <!-- Background image -->
        <div class="col-xl-8 col-lg-6 col-md-5 p-0 d-md-block d-lg-block d-sm-none d-none">
          <div
            class="lavalite-bg"
          >
            <div id="imagesCarrousel">
              <slot name="loginMedia">
                <!-- display from the last to the first one, first one must have class "bottom" -->
                <img class="bottom"
                  v-bind:src="$opensilex.getResourceURI('images/lac.jpg')"
                />

                <img class="top"
                  v-bind:src="$opensilex.getResourceURI('images/vitioeno.jpg')"
                />

                <img class="top"
                  v-bind:src="$opensilex.getResourceURI('images/LBE_Reacteur_de_laboratoire.jpg')"
                />

                <img class="top"
                  v-bind:src="$opensilex.getResourceURI('images/phis-login-bg.jpg')"
                />

                <img class="top"
                  v-bind:src="$opensilex.getResourceURI('images/opensilex-login-bg.png')"
                />
              </slot>
            </div>
            <!-- low opacity green layout used for phis bg image-->
            <div class="lavalite-overlay"></div>
          </div>
        </div>

        <div class="col-xl-4 col-lg-6 col-md-7 my-auto p-0">

          <!-- Language selector -->
          <b-dropdown
            id="langDropdown"
            :title="`language - ${this.language}`"
            class="languagesDropdown"
            variant="link"
            right
          >

            <template v-slot:button-content>
              <i class="icon ik ik-globe"></i>
              <span class="hidden-phone">{{
                $t("LoginComponent.language." + language)
              }}</span>
              <span class="show-phone">{{
                $t("LoginComponent.language." + language).substring(0, 2)
              }}</span>
              <i class="ik ik-chevron-down"></i>
            </template>

            <b-dropdown-item
              v-for="item in languages"
              :key="`language-${item}`"
              href="#"
              @click.prevent="setLanguage(item)"
              >{{ $t("LoginComponent.language." + item) }}
            </b-dropdown-item>
          </b-dropdown>

          <div class="authentication-form mx-auto">
            <!-- Logo -->
            <div class="logo-centered">
              <slot name="loginLogo">
                <img
                  v-bind:src="
                    $opensilex.getResourceURI('images/logo-opensilex.png')
                  "
                  alt
                />
              </slot>
            </div>

            <!-- Guest Connection -->
            <span v-if="connectAsGuest">
              <div class="trademark">
                <slot name="guestLogin">
                  <p>
                    {{$t('LoginComponent.infoGuest')}}
                  </p>
                </slot>
                <b-button
                  class="greenThemeColor"
                  @click="onLoginAsGuest"
                  v-text="$t('LoginComponent.loginAsGuest')"
                ></b-button>
              </div>
              <br>
            </span>

            <!-- Form -->
            <opensilex-SelectForm
              v-if="connectionOptions.length > 1"
              :label="$t('LoginComponent.selectLoginMethod')"
              :options="connectionOptions"
              :selected.sync="loginMethod"
              @select="loginMethodChange"
            ></opensilex-SelectForm>
            <ValidationObserver v-if="loginMethod == 'password'" ref="validatorRef">
              <b-form @submit.prevent="onLogin" class="fullmodal-form">
                <b-form-group id="login-group" required>
                  <ValidationProvider
                    :name="$t('component.login.validation.email')"
                    rules="required|emailOrUrl"
                    v-slot="{ errors }"
                  >
                    <b-form-input
                      id="email"
                      v-model="form.email"
                      required
                      :placeholder="$t('LoginComponent.email')"
                    ></b-form-input>
                    <i class="ik ik-user"></i>
                    <div
                      v-if="errors.length > 0"
                      class="error-message alert alert-danger"
                    >{{ errors[0] }}</div>
                  </ValidationProvider>
                </b-form-group>

                <b-form-group id="password-group" required>
                  <ValidationProvider
                    :name="$t('component.login.validation.password')"
                    rules="required"
                    v-slot="{ errors }"
                  >
                    <b-form-input
                      id="password"
                      type="password"
                      v-model="form.password"
                      required
                      :placeholder="$t('LoginComponent.password')"
                    ></b-form-input>
                    <i class="ik ik-lock"></i>
                    <div
                      v-if="errors.length > 0"
                      class="error-message alert alert-danger"
                    >{{ errors[0] }}</div>
                  </ValidationProvider>
                </b-form-group>
                <a
                    v-if="isResetPassword()"
                    :href="resetPasswordPath"
                >
                  <span>{{ $t('LoginComponent.forgotPassword') }}</span>
                </a>
                <div class="sign-btn text-center">
                  <b-button
                    type="submit"
                    class="greenThemeColor"
                    v-text="$t('component.login.button.login')"
                  ></b-button>
                </div>
              </b-form>
            </ValidationObserver>

            <div class="trademark">
              <slot name="loginFooter">   
                <p>
                  {{ $t("LoginComponent.copyright.3", {
                    version: this.versionInfo.version
                  }) }}
                  <br />
                  {{
                  $t("component.login.copyright.4", {
                    version: this.versionInfo.version
                  })
                  }}
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
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { User } from "../../models/User";
import { TokenGetDTO, AuthenticationService } from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
import { FrontConfigDTO } from "../../lib";
import { SystemService, VersionInfoDTO } from "opensilex-core/index";
import VueRouter from 'vue-router';

@Component
export default class DefaultLoginComponent extends Vue { 
  service: SystemService;
  versionInfo: VersionInfoDTO = {};
  $store: any;
  $router: VueRouter;
  $t: any;
  $i18n: any;
  $opensilex: OpenSilexVuePlugin;
  private langUnwatcher;

  get form() {
    return {
      email: "",
      password: ""
    };
  }

  /**
   * Return the current i18n language
   */
  get language() {
    return this.$i18n.locale;
  }

  /**
   * Return all available languages
   */
  get languages() {
    return Object.keys(this.$i18n.messages);
  }

  /**
   * Set the current i18n language
   */
  setLanguage(lang: string) {
    this.$i18n.locale = lang;
    this.$store.commit("lang", lang);
  }

  get user() {
    return this.$store.state.user;
  }

  /**
   * Ability to be logged as guest
   */
  get connectAsGuest(): boolean {
    let config: FrontConfigDTO = this.$opensilex.getConfig();
    if (config.connectAsGuest === true) {
      return true;
    } 
    return false;
  }

  getPHISModuleVersion(){
    for(let module_version_index in this.versionInfo.modules_version){
      let module = this.versionInfo.modules_version[module_version_index]

      console.log(module)
      if(module.name.includes("PhisWsModule")){
        return module.version;
      }
    }
    return 'Version undefined'
  }

  loginMethod = "password";

  get connectionOptions() {
    let options = [
      {
        id: "password",
        label: this.$t("LoginComponent.passwordConnectionTitle")
      }
    ];

    let opensilexConfig: FrontConfigDTO = this.$opensilex.getConfig();

    if (opensilexConfig.openIDAuthenticationURI) {
      options.push({
        id: "openid",
        label: opensilexConfig.openIDConnectionTitle ?? this.$t("LoginComponent.defaultOpenIDConnectionTitle")
      });
    }

    if (opensilexConfig.samlProxyLoginURI) {
      options.push({
        id: "shibboleth",
        label: opensilexConfig.samlConnectionTitle ?? this.$t("LoginComponent.defaultSAMLConnectionTitle")
      })
    }

    return options;
  }

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
  }

  loginMethodChange(loginMethod) {
    let opensilexConfig: FrontConfigDTO = this.$opensilex.getConfig();
    if (loginMethod.id === "openid") {
      window.location.href = opensilexConfig.openIDAuthenticationURI;
    } else if (loginMethod.id === "shibboleth") {
      window.location.href = opensilexConfig.samlProxyLoginURI;
    } else if (loginMethod.id === "password") {
      this.validatorRef.reset();
    }
  }

  isResetPassword() {
    let opensilexConfig: FrontConfigDTO = this.$opensilex.getConfig();
    return opensilexConfig.activateResetPassword;
  }

  get resetPasswordPath() {
    return this.$router.resolve("/forgot-password").href;
  }

  static async asyncInit($opensilex: OpenSilexVuePlugin) {
    await $opensilex.loadService("opensilex-security.AuthenticationService");
  }

  logout() {
    this.$store.commit("logout");
    this.$router.push("/");
  }

  @Ref("validatorRef") readonly validatorRef!: any;

  forceRefresh = false;
  onLogin() {
    let validatorRef: any = this.validatorRef;
    validatorRef.validate().then(isValid => {
      if (isValid) {
        this.$opensilex.showLoader();
        this.$opensilex
          .getService<AuthenticationService>(
            "opensilex-security.AuthenticationService"
          )
          .authenticate({
            identifier: this.form.email,
            password: this.form.password
          })
          .then((http: HttpResponse<OpenSilexResponse<TokenGetDTO>>) => {
            let user = User.fromToken(http.response.result.token);
            this.$opensilex.setCookieValue(user);
            this.forceRefresh = true;
            this.$store.commit("login", user);
            this.$store.commit("refresh");
          })
          .catch(error => {
            if (error.status == 403) {
              console.error("Invalid credentials", error);
              this.$opensilex.errorHandler(
                error,
                this.$t("component.login.errors.invalid-credentials")
              );
            } else {
              this.$opensilex.errorHandler(error);
            }
            this.$opensilex.hideLoader();
          });
      }
    });
  }

  onLoginAsGuest() {
    this.form.email = "guest@opensilex.org";
    this.form.password = "guest";
    console.log("this.form", this.form)
    this.login().then(() => {
      this.form.email = "";
      this.form.password = "";
    });
  }

  login() {
    this.$opensilex.showLoader();
    return this.$opensilex
      .getService<AuthenticationService>(
        "opensilex-security.AuthenticationService"
      )
      .authenticate({
        identifier: this.form.email,
        password: this.form.password,
      })
      .then((http: HttpResponse<OpenSilexResponse<TokenGetDTO>>) => {
        let user = this.$opensilex.fromToken(http.response.result.token);
        this.$opensilex.setCookieValue(user);
        this.forceRefresh = true;
        this.$store.commit("login", user);
        this.$store.commit("refresh");
      })
      .catch((error) => {
        if (error.status == 403) {
          console.error("Invalid credentials", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.login.errors.invalid-credentials")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
        this.$opensilex.hideLoader();
      });
  }

}
</script>

<style scoped lang="scss">
.fullmodal {
  display: block;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  width: 100%;
  z-index: 9999;
}

.logo-centered > img {
  display: inline-block;
}

.authentication-form .error-message {
  top: 37px;
}

.authentication-form fieldset {
  margin-bottom: 25px;
}

#imagesCarrousel img {

  position:absolute;
  width: 100%;
  height: 100%;
  -webkit-background-size: cover;
  background-size: cover;

  background-repeat: no-repeat;
  left:0;
  -webkit-transition: opacity 2s ease-in-out;
  -moz-transition: opacity 2s ease-in-out;
  -o-transition: opacity 2s ease-in-out;
  transition: opacity 2s ease-in-out;
}

#imagesCarrousel img.top {
animation-name: LoginImageAnimation;
animation-timing-function: ease-in-out;
animation-iteration-count: infinite;
animation-duration: 17s;
animation-direction: normal;
}

.languagesDropdown {
  position: fixed;
  top: 5px;
  right: 5px;
}

@keyframes LoginImageAnimation {
  0% {
    opacity:1;
  }
  17% {
    opacity:1;
  }
  25% {
    opacity:0;
  }
  92% {
    opacity:0;
  }
  100% {
    opacity:1;
  }
}

#imagesCarrousel img:nth-of-type(1) {
  animation-delay: 12s;
}
#imagesCarrousel img:nth-of-type(2) {
  animation-delay: 9s;
}
#imagesCarrousel img:nth-of-type(3) {
  animation-delay: 6s;
}
#imagesCarrousel img:nth-of-type(4) {
  animation-delay: 3s;
}
#imagesCarrousel img:nth-of-type(5) {
  animation-delay: 0;
}


</style>

<i18n>
en:
  LoginComponent:
    selectLoginMethod: Select login method
    passwordConnectionTitle: Connect with password
    forgotPassword: Forgot your password ?
    defaultOpenIDConnectionTitle: Log in with SSO (OpenID)
    defaultSAMLConnectionTitle: Log in with SSO (SAML)
    infoGuest: You can connect as guest
    loginAsGuest: Connect as guest
    email: Email or URI
    password: Password
    language:
      fr: French
      en: English
    copyright: 
      1: PHIS - Phenotyping Hybrid Information System
      2: Version {version}
      3: Based on OpenSILEX version {version}
      4: Copyright ©2021 INRAE
fr:
  LoginComponent:
    selectLoginMethod: Choisir la méthode de connexion
    passwordConnectionTitle: Connexion par mot de passe
    forgotPassword: Mot de passe oublié ?
    defaultOpenIDConnectionTitle: Connexion par SSO (OpenID)
    defaultSAMLConnectionTitle: Connexion par SSO (SAML)
    infoGuest: Vous pouvez vous connecter en tant qu'invité
    loginAsGuest: Connexion en tant qu'invité
    email: Email ou URI
    password: Mot de passe
    language:
      fr: Français
      en: Anglais
    copyright: 
      1: PHIS - Phenotyping Hybrid Information System
      2: Version {version}
      3: Basé sur OpenSILEX version {version}
      4: Copyright ©2021 INRAE
</i18n>