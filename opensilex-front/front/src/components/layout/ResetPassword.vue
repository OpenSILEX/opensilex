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
        <h2>{{ t('component.reset-password.title') }}</h2>
        <n-form
            v-if="!badToken"
            :model="formModel"
            :rules="rules"
            ref="form"
        >
          <n-form-item path="password">
            <InputForm
                v-model:value="formModel.password"
                :label="t('component.reset-password.input')"
                type="password"
                :placeholder="t('component.account.form-password-placeholder')"
            ></InputForm>
          </n-form-item>
          <n-form-item path="confirmation">
            <InputForm
                v-model:value="formModel.confirmation"
                :label="t('component.reset-password.confirm')"
                type="password"
                :disabled="!formModel.password"
                :placeholder="t('component.account.form-password-placeholder')"
            ></InputForm>
          </n-form-item>
          <Button
              variant="primary"
              :label="t('component.reset-password.submit')"
              @click="submit"
          ></Button>
        </n-form>
        <div v-else>
          <h4>{{ t('component.reset-password.error.bad-token') }}</h4>
          <p>{{ t('component.reset-password.error.bad-token-info') }}</p>
          <Button
              variant="secondary"
              :label="t('component.reset-password.return')"
              @click="router.push({ path: '/' })"
          ></Button>
        </div>
      </div>
      <div class="col"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {AuthenticationService} from "opensilex-security/api/authentication.service";
import {useRoute, useRouter} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {FormItemRule, FormRules, NForm, NFormItem} from "naive-ui";
import InputForm from "@/components/common/forms/InputForm.vue";
import Button from "@/components/common/buttons/Button.vue";


//#region public

//#endregion

//#region private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const service = opensilex.getService<AuthenticationService>("opensilex-security.AuthenticationService");
const router = useRouter();
const route = useRoute();
const store = useStore();
const {t, locale, availableLocales} = useI18n();

const form = useTemplateRef<InstanceType<typeof NForm>>("form");

const formModel = ref({
  password: "",
  confirmation: ""
});
const passwordToken = ref<string | null>();
const badToken = ref<boolean>(false);

const rules: FormRules = {
  password: {
    required: true,
    message: t('component.reset-password.error.password-required'),
    trigger: ['input', 'blur']
  },
  confirmation: {
    validator: validateSamePassword,
    message: t('component.reset-password.error.confirmation-required'),
    trigger: ['input', 'blur']
  }
};

const languages = computed(() =>
    availableLocales.map(l => ({
      key: l,
      label: t(`component.header.language.${l}`)
    }))
);

onMounted(() => {
  passwordToken.value = decodeURIComponent(route.params.uri as string);
  if (!passwordToken.value) {
    router.push({path: "/"});
  }
  service.renewPassword(passwordToken.value, true, formModel.value.password).catch((error) => {
    if (error.status == 403 || error.status == 500) {
      console.error("Invalid credentials", error);
      opensilex.errorHandler(error, t("component.login.errors.invalid-credentials"));
    } else {
      if (error.status == 400) {
        console.error("Bad token", error);
        badToken.value = true;
      } else {
        console.error("Error", error);
        opensilex.errorHandler(error);
      }
    }
  });
});

function submit() {
  form.value.validate((errors) => {
    if (errors) {
      console.error("Form errors", errors);
      return;
    }

    service.renewPassword(passwordToken.value, false, formModel.value.password).then((_http) => {
      opensilex.showSuccessToast(t("ResetPasswordComponent.renew-password"));
      passwordToken.value = null;
      router.push({path: "/"});
    }).catch((error) => {
      if (error.status == 400) {
        console.error("Invalid credentials", error);
        opensilex.errorHandler(error, t("component.login.errors.invalid-token"));
      } else {
        console.error("Error", error);
        opensilex.errorHandler(error);
      }
    });
  })
}


function validateSamePassword(_rule: FormItemRule, value: string): boolean {
  return value === formModel.value.password;
}

function onLanguageSelected(newLocale: string) {
  locale.value = newLocale;
  store.commit("lang", newLocale);
}

//#endregion

</script>

<style scoped lang="scss">
</style>