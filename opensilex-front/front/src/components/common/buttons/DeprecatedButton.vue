<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
    <span>
        <opensilex-Icon v-if="!deprecated" icon="bi#bi-dash-circle" />
        <opensilex-Icon v-else icon="bi#bi-plus-circle" />
    </span>
    </template>
  </opensilex-Button>
</template>

<script setup lang="ts">
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
