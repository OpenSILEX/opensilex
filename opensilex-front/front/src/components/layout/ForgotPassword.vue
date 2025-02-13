<template>
  <div class="container-fluid">
    <b-row>
      <b-col></b-col>
      <b-col cols="8">
        <h3>{{ $t("ForgotPasswordComponent.title") }}</h3>
        <br />
        <div>
          <p>
            {{ $t("ForgotPasswordComponent.steps") }}
          </p>
          <ol class="list-unstyled">
            <li>
              <span class="text-primary text-medium">1. </span
              >{{ $t("ForgotPasswordComponent.step-one") }}
            </li>
            <li>
              <span class="text-primary text-medium">2. </span
              >{{ $t("ForgotPasswordComponent.step-two") }}
            </li>
            <li>
              <span class="text-primary text-medium">3. </span
              >{{ $t("ForgotPasswordComponent.step-three") }}
            </li>
          </ol>
        </div>
        <br />
        <ValidationObserver ref="validatorRef">
          <b-form @submit.prevent="onResetPasswordByEmail">
            <!-- Email -->
            <opensilex-InputForm
              :value.sync="email"
              label="ForgotPasswordComponent.enter-email"
              type="email"
              :required="true"
              rules="email"
              placeholder="component.account.form-email-placeholder"
            ></opensilex-InputForm>

            <b-row>
              <b-col>
                <b-button
                  type="submit"
                  variant="primary"
                  v-text="$t('ForgotPasswordComponent.reset-password')"
                ></b-button>
              </b-col>
              <b-col>
                <router-link :to="{ path: '/' }"
                  ><b-button
                    variant="secondary"
                    v-text="$t('ForgotPasswordComponent.returnHome')"
                  ></b-button
                ></router-link>
              </b-col>
            </b-row>
          </b-form>
        </ValidationObserver>
      </b-col>
      <b-col cols="3"></b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue, { defineComponent } from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
// @ts-ignore
import { AuthenticationService } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";

export default defineComponent({
    data() {
        const $opensilex: OpenSilexVuePlugin = undefined;
        const $router: any = undefined;
        const $store: any = undefined;
        const $t: any = undefined;
        const email: string = null;

        return {
            email,
            $t,
            $store,
            $router,
            $opensilex
        };
    },
    computed: {
        user() {
            return this.$store.state.user;
        },
        validatorRef: {
            cache: false,
            get() {
                return this.$refs["validatorRef"] as any;
            }
        }
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
            this.$opensilex
            .getService<AuthenticationService>(
            "opensilex-security.AuthenticationService"
            )
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
