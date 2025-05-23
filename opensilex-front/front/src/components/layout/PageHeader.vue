<template>
  <div class="page-header">
    <opensilex-Icon v-if="hasIcon" :icon="icon" class="title-icon" />
    <img v-else-if="hasImage" :src="imageSrc" class="title-image" />

    <div class="header-title">
      <h5>
        <span class="title-title">
          <slot name="title">
            {{ t(title || 'default.title') }} 
          </slot>
        </span>
      </h5>
    </div>

    <div class="header-desc">
      <span class="title-description">
        <slot name="description">
          {{ t(description || 'default.description') }} 
        </slot>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts"> 
import { useI18n } from "vue-i18n";

const { t } = useI18n();

defineProps<{
  icon?: any;
  title?: string;
  description?: string;
  isExperimentalFeature?: boolean;
  hasIcon?: boolean;
  hasImage?: boolean;
  imageSrc?: string;
}>();
</script>

<style scoped lang="scss">
/* Styles inchangés */
.page-header {
  z-index: 1030;
}

.title-icon {
  float: left;
  width: 40px;
  height: 40px;
  border-radius: 5px;
  margin-right: 20px;
  vertical-align: middle;
  font-size: 22px;
  color: #fff;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 12px -3px rgba(0, 0, 0, 0.5);
  background-color: #00a38d;
}

.title-image {
  float: left;
  width: 9em;
  margin-right: 20px;
  vertical-align: middle;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}

h5 {
  margin-bottom: 0;
}
.header-title, .header-desc, .title-description {
  width: 80%;
}

.navbar-title {
  white-space: pre;
}

/* Les autres styles ne changent pas */
</style>
