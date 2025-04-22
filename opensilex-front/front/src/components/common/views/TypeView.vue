<template>
  <div class="static-field">
    <span :class="['field-view-title', customClass]">{{ t("component.common.type") }}</span>
    <span class="static-field-line">
      <opensilex-Icon :icon="$opensilex?.getRDFIcon(type)" />
      <!-- &nbsp; -->
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
  customClass?: string;
}>();

const { t } = useI18n();

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const customClass = props.customClass || '';
</script>

<style scoped lang="scss">

.sectionTitle {
  font-weight: bold;
  min-width: 60px;
  display:inline-block
  // margin-right: 10px;
}
</style>
