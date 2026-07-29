<template>
  <div class="static-field">
    <span :class="['field-view-title', customClass]">{{ $t(title) }}</span>
    <span class="static-field-line">
      <UriLink
        :uri="uri"
        :value="value"
        :url="url"
        :to="to"
        @linkClicked="$emit('linkClicked')"
        :allowCopy="allowCopy"
      />
    </span>
  </div>
</template>

<script setup lang="ts">
import UriLink from "@/components/common/views/UriLink.vue";

//#region Public
const props = withDefaults(defineProps<{
  uri: string;
  value?: string;
  url?: string;
  to?: string;
  title?: string;
  customClass?: string;
  allowCopy?: boolean;
}>(), {
  title: 'component.common.uri',
  customClass: '',
  allowCopy: true
});

const emit = defineEmits<{
  (e: 'linkClicked'): void;
}>();
</script>
//#endregion

<style scoped lang="scss">
.static-field-line {
  word-wrap: normal;
}

.specificProperties {
  min-width: 250px;
}

.sectionTitle {
  font-weight: bold;
  min-width: 60px;
  display:inline-block
}
</style>
