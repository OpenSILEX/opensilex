<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col"></div>
      <div class="col-8">
        <h3>{{ t("ForgotPasswordComponent.title") }}</h3>
        <br/>
        <div>
          <p>
            {{ t("ForgotPasswordComponent.steps") }}
          </p>
          <ol class="list-unstyled">
            <li>
              <span class="text-primary text-medium">1. </span
              >{{ t("ForgotPasswordComponent.step-one") }}
            </li>
            <li>
              <span class="text-primary text-medium">2. </span
              >{{ t("ForgotPasswordComponent.step-two") }}
            </li>
            <li>
              <span class="text-primary text-medium">3. </span
              >{{ t("ForgotPasswordComponent.step-three") }}
            </li>
          </ol>
        </div>
        <br/>
        <div>
          <!-- Email -->
          <div class="row mb-4">
            <div class="form-group">
              <opensilex-InputForm
                  :value.sync="email"
                  :label="t('ForgotPasswordComponent.enter-email')"
                  type="email"
                  :required="true"
                  rules="email"
                  :placeholder="t('component.account.form-email-placeholder')"
              ></opensilex-InputForm>
            </div>
          </div>

          <div class="row">
            <div class="col">
              <button
                  class="btn btn-primary"
                  type="submit"
                  v-text="t('ForgotPasswordComponent.reset-password')"
              ></button>
            </div>
            <div class="col">
              <router-link :to="{ path: '/' }"
              >
                <button
                    class="btn btn-secondary"
                    v-text="t('ForgotPasswordComponent.returnHome')"
                ></button
                >
              </router-link>
            </div>
          </div>
        </div>
      </div>
      <div class="col"></div>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, inject, ref} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {AuthenticationService} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {useRouter} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";

export default defineComponent({
  name: 'forgotPassword',
  props: {},
  setup() {
    const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
    const router = useRouter();
    const store = useStore();
    const {t} = useI18n();
    const user = computed(() => store.state.user);
    const authenticationService = opensilex.getService<AuthenticationService>("opensilex-security.AuthenticationService");
    const email = ref<string>("");

    return {
      opensilex,
      router,
      store,
      t,
      user,
      authenticationService,
      email
    };
  },
  methods: {
    async asyncInit($opensilex: OpenSilexVuePlugin) {
      await $opensilex.loadService("opensilex-security.AuthenticationService");
    },
    onResetPasswordByEmail() {
      let validatorRef: any = this.validatorRef;
      validatorRef.validate().then((isValid) => {
        if (isValid) {
          this.resetPasswordByEmail();
        }
      });
    },
    resetPasswordByEmail() {
      this.authenticationService
          .forgotPassword(this.email)
          .then((http: HttpResponse<OpenSilexResponse<any>>) => {
            this.$opensilex.showSuccessToastWithDelay(
                this.$t("ForgotPasswordComponent.link-email"),
                5000
            );
          })
          .catch((error) => {
            if (error.status == 503) {
              console.error("Service not available", error);
              this.$opensilex.errorHandler(
                  error,
                  this.$t("ForgotPasswordComponent.service-not-available")
              );
            } else if (error.status == 403 || error.status == 500) {
              console.error("Invalid credentials", error);
              this.$opensilex.errorHandler(
                  error,
                  this.$t("ForgotPasswordComponent.invalid-identifier")
              );
            } else {
              console.log(error);
              this.$opensilex.errorHandler(error);
            }
            this.$opensilex.hideLoader();
          });
    }
  }
})

</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ForgotPasswordComponent:
    title: "Forgot password ?"
    enter-email: Enter the e-mail linked to your account
    reset-password: Send an e-mail to this adress
    steps: "Change your password in three easy steps. This will help you to secure your password!"
    step-one: "Enter your email address below."
    step-two: "Our system will send you a temporary link."
    step-three: "Use the link to reset your password."
    service-not-available: Service not available
    invalid-identifier: Identifiant invalide
    link-email: An e-mail has been sent to you
    returnHome: return to homepage
fr:
  ForgotPasswordComponent:
    title: "Mot de passe oublié ?"
    enter-email: Entrez l'adresse e-mail liée à votre compte
    reset-password: Envoyer un e-mail à cette adresse
    steps: "Changez votre mot de passe en trois étapes simples. Cela vous aidera à sécuriser votre mot de passe !"
    step-one: "Saisissez votre adresse électronique ci-dessous."
    step-two: "Notre système vous enverra un lien temporaire."
    step-three: "Utilisez le lien pour réinitialiser votre mot de passe."
    service-not-available: Service not available
    invalid-identifier: Identifiant invalide
    link-email: Un email vous a été envoyé
    returnHome: Retourner à la page d'accueil
</i18n>
