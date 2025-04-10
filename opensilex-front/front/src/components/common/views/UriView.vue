<template>
  <div class="static-field">
    <span :class="['field-view-title', customClass]">{{ $t(title) }}</span>
    <span class="static-field-line">
      <opensilex-UriLink
        :uri="uri"
        :value="value"
        :url="url"
        :to="to"
        @linkClicked="$emit('linkClicked')"
      />
    </span>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue';

// props
const props = defineProps<{
  uri: string;
  value: string;
  url?: string;
  to?: string;
  title?: string;
  customClass?: string;
}>();

// default value
const title = props.title || 'component.common.uri';
const customClass = props.customClass || '';

//  events
const emit = defineEmits<{
  (e: 'linkClicked'): void;
}>();
</script>

<style scoped lang="scss">
.static-field-line {
  word-wrap: normal;
}

.specificProperties {
  min-width: 250px;
}
</style>
