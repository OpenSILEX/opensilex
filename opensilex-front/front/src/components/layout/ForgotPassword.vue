<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col text-end">
        <div class="m-2">
          <n-dropdown
              trigger="click"
              :options="languages"
              @select="onLanguageSelected"
          >
            <n-button><i class="bi bi-globe m-2"></i> {{ t(`component.header.language.${locale}`) }}</n-button>
          </n-dropdown>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col"></div>
      <div class="col-8">
        <h3>{{ t("component.forgot-password.title") }}</h3>
        <br/>
        <div>
          <p>
            {{ t("component.forgot-password.steps") }}
          </p>
          <ol class="list-unstyled">
            <li>
              <span class="text-primary text-medium">1. </span
              >{{ t("component.forgot-password.step-one") }}
            </li>
            <li>
              <span class="text-primary text-medium">2. </span
              >{{ t("component.forgot-password.step-two") }}
            </li>
            <li>
              <span class="text-primary text-medium">3. </span
              >{{ t("component.forgot-password.step-three") }}
            </li>
          </ol>
        </div>
        <br/>
        <div>
          <!-- Email -->
          <div class="row mb-4">
            <div class="form-group">
              <n-form :model="formModel" :rules="rules">
                <n-form-item path="email">
                  <InputForm
                      v-model:value="formModel.email"
                      :label="t('component.forgot-password.enter-email')"
                      type="email"
                      :required="true"
                      :placeholder="t('component.account.form-email-placeholder')"
                  ></InputForm>
                </n-form-item>
              </n-form>
            </div>
          </div>
        </div>

        <div class="row">
          <div class="col">
            <button
                class="btn btn-primary"
                type="submit"
                v-text="t('component.forgot-password.reset-password')"
                @click="resetPasswordByEmail"
            ></button>
          </div>
          <div class="col">
            <router-link :to="{ path: '/' }"
            >
              <button
                  class="btn btn-secondary"
                  v-text="t('component.forgot-password.returnHome')"
              ></button
              >
            </router-link>
          </div>
        </div>
      </div>
      <div class="col"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, defineComponent, inject, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {AuthenticationService} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import InputForm from "@/components/common/forms/InputForm.vue";
import {requiredTrimmed, validEmail} from "@/models/FormFieldsFormatter";
import {NForm, NFormItem, NDropdown, NButton} from "naive-ui";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const {t, locale, availableLocales} = useI18n();
const authenticationService = opensilex.getService<AuthenticationService>("opensilex-security.AuthenticationService");
const formModel = ref({
  email: ""
});
const languages = computed(() =>
    availableLocales.map(l => ({
      key: l,
      label: t(`component.header.language.${l}`)
    })));
const rules = {
  email: [validEmail(), requiredTrimmed('component.account.email-address')]
};

function resetPasswordByEmail() {
  if (!formModel.value.email) {
    opensilex.showErrorToast(t("component.forgot-password.empty-email"));
    return;
  }
  authenticationService
      .forgotPassword(formModel.value.email)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        opensilex.showSuccessToast(
            t("component.forgot-password.link-email")
        );
      })
      .catch((error) => {
        if (error.status == 503) {
          console.error("Service not available", error);
          opensilex.errorHandler(
              error,
              t("component.forgot-password.service-not-available")
          );
        } else if (error.status == 403 || error.status == 500) {
          console.error("Invalid credentials", error);
          opensilex.errorHandler(
              error,
              t("component.forgot-password.invalid-identifier")
          );
        } else {
          console.log(error);
          opensilex.errorHandler(error);
        }
        opensilex.hideLoader();
      });
}

function onLanguageSelected(newLocale: string) {
  locale.value = newLocale;
  store.commit("lang", newLocale);
}
</script>

<style scoped lang="scss">
</style>