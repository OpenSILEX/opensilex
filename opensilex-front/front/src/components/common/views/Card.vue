<template>
  <div class="card" @click="emit('click')">
    <div v-if="!noHeader" class="card-header">

      <h3 class="capitalize-first-letter">
      <!-- si le parent passe une propriété "title" -->
        <slot name="title">
          <opensilex-Icon
            v-if="icon && icon.startsWith('fa#')"
            :icon="icon"
            class="icon-title"
          />
          <i
            v-else-if="icon && icon.startsWith('bi-')"
            :class="['icon-title', icon]"
          ></i>
          {{ t(label || '') }}
        </slot>
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

// pour afficher les boutons en  une lignes dans la carte de détail d'une variable par exemple 
.card-header-right {
  display: flex !important;
}
</style>
