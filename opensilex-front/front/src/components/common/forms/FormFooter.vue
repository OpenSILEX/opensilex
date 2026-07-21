<template>
  <div class="form-footer flex gap-2 justify-end">
    <slot />
    <button v-if="showCancel" class="btn btn-secondary" @click="$emit('cancel')">{{ t(cancelLabel) }}</button>
    <button class="btn greenThemeColor" @click="$emit('submit')">{{ t(submitLabel) }}</button>
  </div>
</template>

<script setup lang="ts">
import {useI18n} from 'vue-i18n'
const { t } = useI18n()
const props = defineProps({
  cancelLabel: { type: String, default: 'component.common.cancel' },
  submitLabel: { type: String, default: 'component.common.ok' },
  showCancel: { type: Boolean, default: true }
})
</script>

<style scoped lang="scss">
.form-footer { padding-top: 1rem; }
</style>
