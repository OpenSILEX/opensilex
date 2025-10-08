<template>
  <div class="static-field">
    <span class="field-view-title" :class="{ 'whole-line': !inline }">
      {{ t(label) }}
    </span>

    <!-- Liste inline (séparée par des virgules) -->
    <span class="static-field-line" v-if="inline">
      <template v-for="(value, index) in list" :key="value.uri || index">
        <opensilex-UriLink v-bind="value" :allowCopy="allowCopy" />
        <span v-if="index + 1 < list.length">, </span>
      </template>
    </span>

    <!-- Liste à puces -->
    <ul v-else>
      <li v-for="(value, index) in list" :key="value.uri || index">
        <opensilex-UriLink v-bind="value" />
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { withDefaults, defineProps } from 'vue'
import { useI18n } from 'vue-i18n'
import type { UriLinkDestination } from './UriLink.vue'

export interface UriLinkDescription {
  uri: string
  value: string
  url?: string
  to: UriLinkDestination
}

const props = withDefaults(defineProps<{
    label: string
  list: UriLinkDescription[]
  allowCopy: boolean
  inline?: boolean
}>(), {
  inline: true,
  list: () => []
})

// la on utilise le scope GLOBAL, car la clé "label" vient du parent.
// (peut etre un moyen de passer des clé de trad entre composants. À approfondir)
const { t } = useI18n({ useScope: 'global' })
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
.whole-line {
  width: 100%;
}
</style>
