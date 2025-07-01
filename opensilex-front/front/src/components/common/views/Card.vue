<template>
  <div class="card" @click="emit('click')">
    <div v-if="!noHeader" class="card-header">

      <!-- si le parent passe une propriété "title" -->
      <h3 class="capitalize-first-letter">
        <slot name="title">
          <opensilex-Icon v-if="icon && icon.startsWith('fa#')" :icon="icon" class="icon-title" />
          <i v-else-if="icon && icon.startsWith('bi-')" :class="['icon-title', icon]"></i>
          {{ t(label || '') }}
        </slot>
      </h3>

      <!-- si le parent passe une propriété "label" -->
      <h3 class="capitalize-first-letter">
        <opensilex-Icon v-if="icon && icon.startsWith('fa#')" :icon="icon" class="icon-title" />
        <i v-else-if="icon && icon.startsWith('bi-')" :class="['icon-title', icon]"></i>
        <!-- {{ $t(label) }} -->
         {{ t(label || '') }} 
      </h3>

      <slot name="header"></slot>

      <div class="card-header-right">
        <slot name="rightHeader"></slot>
      </div>

    </div>

    <div class="card-body">
      <slot name="body"></slot>
    </div>

    <div v-if="!noFooter" class="card-footer text-center">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
 
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = defineProps<{
  noFooter?: boolean;
  noHeader?: boolean;
  label: string;
  icon?: string;
}>();

const emit = defineEmits<{
  (event: "click"): void;
}>();
</script>

<style scoped lang="scss">
</style>
