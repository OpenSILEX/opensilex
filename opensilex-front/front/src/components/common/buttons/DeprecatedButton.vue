<template>
  <OVLButton
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
    <span>
        <OVLIcon v-if="!deprecated" icon="bi#bi-dash-circle" />
        <OVLIcon v-else icon="bi#bi-plus-circle" />
    </span>
    </template>
  </OVLButton>
</template>

<script setup lang="ts">
import { OVLButton, OVLIcon } from "opensilex-vue-library";
import { computed, defineProps, defineEmits } from 'vue'
import { useI18n } from 'vue-i18n'

const emit = defineEmits(['click'])

const props = defineProps({
  small: Boolean,
  disabled: Boolean,
  deprecated: Boolean
})

const { t } = useI18n()

const label = computed(() =>
  props.deprecated
    ? t('DeprecatedButton.cancelDeprecated')
    : t('DeprecatedButton.deprecated')
)
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DeprecatedButton:
    deprecated: Deprecated
    cancelDeprecated: Cancel Deprecated

fr:
  DeprecatedButton:
    deprecated: Obsolète
    cancelDeprecated: Annuler Obsolète
</i18n>
