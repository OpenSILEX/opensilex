<template>
  <div class="col col-12"
       v-bind:class="{
          'col-xl-3 col-sm-6': baseWidth,
          'col-xl-12 col-sm-12': fullWidth,
          'col-xl-6 col-sm-6': halfWidth,
          'col-xl-3 col-sm-3': quarterWidth
       }">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import {useI18n} from "vue-i18n";
import {computed} from "vue";

const {t} = useI18n();

const props = withDefaults(defineProps<{
  fullWidth?: boolean
  halfWidth?: boolean
  quarterWidth?: boolean
}>(), {
  fullWidth: false,
  halfWidth: false,
  quarterWidth: false
})

const baseWidth = computed(() => !props.fullWidth && !props.halfWidth && !props.quarterWidth)
</script>

<style scoped lang="scss">

</style>