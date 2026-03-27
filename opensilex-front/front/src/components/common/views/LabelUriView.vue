<template>
  <div class="static-field">
    <span class="field-view-title">{{ t(label) }}</span>
    <span class="static-field-line">{{ value }}</span>
    <opensilex-UriLink
      class="uri"
      :uri="uri"
      :url="uri"
      :value="uri"
      :to="to"
      :allowCopy="allowCopy"
    />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from "vue-i18n";

interface Props {
  label?: string;
  value?: string;
  uri?: string;
  to?: string;
  allowCopy?: boolean;
}

withDefaults(defineProps<Props>(), {
  allowCopy: true,
});

const { t } = useI18n();
</script>

<style scoped lang="scss">
.static-field .uri {
  padding-left: 20px;
}
</style>