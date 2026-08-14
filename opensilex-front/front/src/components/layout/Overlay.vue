<template>
  <n-spin
    :show="show"
  >
    <slot></slot>
  </n-spin>
</template>

<script setup lang="ts">
import { NSpin } from "naive-ui";

const props = withDefaults(defineProps<{
  show: boolean
}>(), {
  show: false
});

</script>

<style scoped lang="scss">
</style>
