<template>
  <div class="container-fluid">
    <b-row>
      <b-col></b-col>
      <b-col cols="8">
        <h2>{{ $t("ResetPasswordComponent.title") }}</h2>
        <br />
        <div v-if="!badToken">
          <ValidationObserver ref="validatorRef">
            <b-form @submit.prevent="onResetPassword">
              <opensilex-InputForm
                :value.sync="password"
                label="ResetPasswordComponent.new-password"
                type="password"
                vid="password-first"
                :required="true"
                placeholder="component.user.form-password-placeholder"
                autocomplete="new-password"
              ></opensilex-InputForm>
              <opensilex-InputForm
                :value.sync="confirmation"
                label="ResetPasswordComponent.confirm-password"
                vid="confirmation"
                type="password"
                :required="true"
                rules="required|confirmed:password-first"
                placeholder="component.user.form-password-placeholder"
                autocomplete="new-password"
              ></opensilex-InputForm>

              <div class="sign-btn text-center">
                <b-button
                  type="submit"
                  variant="primary"
                  v-text="$t('ResetPasswordComponent.reset-password')"
                ></b-button>
              </div>
            </b-form>
          </ValidationObserver>
        </div>
        <div v-else>
          <h4>
            <b-alert variant="warning" show>{{
              $t("ResetPasswordComponent.bad-token")
            }}</b-alert>
          </h4>

          <p v-html="$t('ResetPasswordComponent.bad-token-info')"></p>
          <router-link :to="{ path: '/' }"
            ><b-button
              variant="secondary"
              v-text="$t('ForgotPasswordComponent.returnHome')"
            ></b-button
          ></router-link>
        </div>
      </b-col>
      <b-col cols="3"></b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

import { AuthenticationService } from "opensilex-security/index";

import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";

@Component
export default class ResetPassword extends Vue {
  $t: any;
  $store: any;
  $router: any;
  service: AuthenticationService;

  passwordToken: any = null;
  password: string = null;
  confirmation: string = null;
  badToken = false;

  $opensilex: OpenSilexVuePlugin;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex-security.AuthenticationService"
    );
  }

  mounted() {
    this.passwordToken = decodeURIComponent(this.$route.params.uri);
    if (this.passwordToken == null || this.passwordToken == undefined) {
      this.$router.push({ path: "/" });
    }
    console.debug("Renew token :" + this.passwordToken);
    // test if renew token exist
    this.service
      .renewPassword(this.passwordToken, true, this.password)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        console.debug("Renew token exist");
      })
      .catch((error) => {
        console.debug(error);
        if (error.status == 403 || error.status == 500) {
          console.error("Invalid credentials", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.login.errors.invalid-credentials")
          );
        } else {
          if (error.status == 400) {
            console.debug(error);
            console.error("Bad token", error);
            this.badToken = true;
          } else {
            this.$opensilex.errorHandler(error);
          }
        }
      });
  }

  static async asyncInit($opensilex: OpenSilexVuePlugin) {
    await $opensilex.loadService("opensilex-security.AuthenticationService");
  } 

  onResetPassword() {
    let validatorRef: any = this.validatorRef;
    validatorRef.validate().then((isValid) => {
      if (isValid) {
        if (this.passwordToken) {
          this.renewPassword();
        }
      }
    });
  }

  renewPassword() {
    console.log(this.passwordToken, false, this.password);
    this.service
      .renewPassword(this.passwordToken, false, this.password)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        this.$opensilex.showSuccessToastWithDelay(
          this.$t("ResetPasswordComponent.renew-password"),
          5000
        );
        this.passwordToken = null;
        this.$router.push({ path: "/" });
      })
      .catch((error) => {
        console.debug(error);

        if (error.status == 400) {
          console.error("Invalid credentials", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.login.errors.invalid-token")
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
</style>

<i18n>
en:
  ResetPasswordComponent:
    bad-token : The url is not valid. 
    bad-token-info: "Attempt to renew your password again or <br>
                     Contact the system administrator."
    title: Password renewal
    new-password: Define a new password
    confirm-password: Confirmez votre mot de passe
    reset-password: Save and go to home
    renew-password: Your password has been successfully modify

fr:
  ResetPasswordComponent:
    bad-token : L'url saise n'est pas valide.
    bad-token-info: "
                Tenter de renouveller à nouveau votre mot de passe ou <br>
                Contactez l'administrateur du système.  " 
    title: Renouvellement de nouveau mot de passe
    new-password: Saisissez un nouveau mot de passe
    confirm-password: Confirmez votre mot de passe
    reset-password: Enregister et revenir à l'acceuil
    renew-password: Votre mot de passe a bien été mis à jour

</i18n>
