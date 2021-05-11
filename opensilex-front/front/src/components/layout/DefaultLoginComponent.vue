<template>
  <div class="fullmodal auth-wrapper" v-if="!user.isLoggedIn() || forceRefresh">
    <div class="container-fluid h-100">
      <div class="row flex-row h-100 bg-white">
        <div class="col-xl-8 col-lg-6 col-md-5 p-0 d-md-block d-lg-block d-sm-none d-none">
          <div
            class="lavalite-bg"
            v-bind:style="{
              'background-image':
                'url(' +
                $opensilex.getResourceURI('images/opensilex-login-bg.jpg') +
                ')',
            }"
          >
            <div class="lavalite-overlay"></div>
          </div>
        </div>
        <div class="col-xl-4 col-lg-6 col-md-7 my-auto p-0">
          <div class="authentication-form mx-auto">
            <div class="logo-centered">
              <img
                v-bind:src="
                  $opensilex.getResourceURI('images/logo-phis-lg.png')
                "
                alt
              />
            </div>
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
                      :placeholder="$t('component.login.input.email')"
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
                      :placeholder="$t('component.login.input.password')"
                    ></b-form-input>
                    <i class="ik ik-lock"></i>
                    <div
                      v-if="errors.length > 0"
                      class="error-message alert alert-danger"
                    >{{ errors[0] }}</div>
                  </ValidationProvider>
                </b-form-group> 
                 <a href="forgot-password" v-if="isResetPassword()"><span>{{$t('LoginComponent.forgotPassword')}}</span></a>
                <div class="sign-btn text-center">
                  <b-button
                    type="submit"
                    variant="primary"
                    v-text="$t('component.login.button.login')"
                  ></b-button>
                </div>
              </b-form>
            </ValidationObserver>
            <div class="trademark">
              <p>
                {{ $t("component.login.copyright.1") }}
                <br />
                {{ $t("component.login.copyright.2") }}
                <br />
                {{
                $t("component.login.copyright.3", {
                version: release.version,
                date: release.date,
                })
                }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { User } from "../../models/User";
// @ts-ignore
import { TokenGetDTO, AuthenticationService } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
import { FrontConfigDTO } from "../../lib";

@Component
export default class DefaultLoginComponent extends Vue {
  get form() {
    return {
      email: "",
      password: ""
    };
  }

  $store: any;
  $router: any;

  get user() {
    return this.$store.state.user;
  }

  get release() {
    return this.$store.state.release;
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
        label: opensilexConfig.openIDConnectionTitle
      });
    }

    return options;
  }

  loginMethodChange(loginMethod) {
    console.error(loginMethod);
    if (loginMethod.id == "openid") {
      let opensilexConfig: FrontConfigDTO = this.$opensilex.getConfig();
      window.location.href = opensilexConfig.openIDAuthenticationURI;
    } else if (loginMethod.id == "password") {
      this.validatorRef.reset();
    }
  }

  isResetPassword(){
    let opensilexConfig: FrontConfigDTO = this.$opensilex.getConfig();
    return opensilexConfig.activateResetPassword;
  }

  $opensilex: OpenSilexVuePlugin;

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
</style>

<i18n>
en:
  LoginComponent:
    selectLoginMethod: Select login method
    passwordConnectionTitle: Connect with password
    forgotPassword: Forgot password ?
fr:
  LoginComponent:
    selectLoginMethod: Choisir la méthode de connexion
    passwordConnectionTitle: Connexion par mot de passe
    forgotPassword: Mot de passe oublié ?
</i18n>
