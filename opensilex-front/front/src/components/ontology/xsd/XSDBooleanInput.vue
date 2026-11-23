<template>
  <n-form-item :show-require-mark="false">
    <n-radio-group v-model:value="internalValue">
      <n-radio value="false">{{ t('component.common.no') }}</n-radio>
      <n-radio value="true">{{ t('component.common.yes') }}</n-radio>
    </n-radio-group>
    <i
      v-if="property.comment"
      class="bi bi-question-circle-fill text-secondary help-icon"
      :title="property.comment"
      tabindex="0"
      aria-hidden="true"
    />
  </n-form-item>
</template>

<script setup lang="ts">
import {computed} from "vue";
import {useI18n} from "vue-i18n";
import {NFormItem, NRadio, NRadioGroup} from "naive-ui";
import type {VueRDFTypePropertyDTO} from "@/lib";

defineProps<{
  property: VueRDFTypePropertyDTO
}>();

const model = defineModel<string | boolean>("value");

const {t} = useI18n();

/** Stored values come back from the API as "true"/"false" strings, so both forms are accepted */
const internalValue = computed<string | undefined>({
  get: () => model.value === undefined || model.value === null ? undefined : String(model.value),
  set: (value) => model.value = value
});
</script>

<style scoped lang="scss">
.help-icon {
  margin-left: 0.5rem;
}
</style>
