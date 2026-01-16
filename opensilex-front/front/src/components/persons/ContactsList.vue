<template>
  <div class="static-field">
    <span class="field-view-title">{{ t(label) }}</span>
    <span class="static-field-line">
      <span v-for="(value, index) in list" :key="value?.uri ?? index">
        <opensilex-PersonContact :personContact="value" />
        <span v-if="index + 1 < list.length">, </span>
      </span>
    </span>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { PersonDTO } from 'opensilex-security/index'

const { t } = useI18n()

withDefaults(
  defineProps<{
    label: string
    list: PersonDTO[]
  }>(),
  {
    list: () => []
  }
)
</script>

<style scoped lang="scss">
.static-field-line {
  margin-right: 3px;
}
</style>
