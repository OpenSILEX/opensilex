<template>
  <div class="static-field">
    <span class="field-view-title">{{ t("component.common.type") }}</span>
    <span class="static-field-line">
      <opensilex-Icon :icon="$opensilex?.getRDFIcon(type)" />&nbsp;
      <opensilex-UriLink
        v-if="copyableTypeUri"
        :uri="$opensilex?.getShortUri(type)"
        :value="typeLabel"
        :allowCopy="copyableTypeUri"
      />
      <span v-else class="capitalize-first-letter">{{ typeLabel }}</span>
    </span>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { useI18n } from 'vue-i18n';
import OpenSilexVuePlugin from '../../models/OpenSilexVuePlugin';

const props = defineProps<{
  type: string;
  typeLabel: string;
  copyableTypeUri?: boolean;
}>();

const { t } = useI18n();

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
</script>

<style scoped lang="scss">
</style>
