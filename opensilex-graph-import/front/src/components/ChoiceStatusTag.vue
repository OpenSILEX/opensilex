<template>
  <n-tag size="small" :type="tagType" :bordered="false">{{ $t('GraphImport.status.' + (status ?? 'UNDECIDED')) }}</n-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChoiceStatus } from '../reasons'

/** The colours of the tag, as naive-ui names them. */
type TagType = 'default' | 'success' | 'info' | 'warning'

//#region Public
const props = defineProps<{
  status?: string
}>()
//#endregion

//#region Private
//#region Data & Computed
const TYPES: Record<ChoiceStatus, TagType> = {
  CHOSEN: 'success',
  DECIDED: 'success',
  SUGGESTED: 'info',
  UNDECIDED: 'warning',
  LEFT_OUT: 'default'
}

const tagType = computed<TagType>(() => TYPES[props.status as ChoiceStatus] ?? 'default')
//#endregion
//#endregion
</script>
