<template>
  <span v-if="value" class="static-field-line">
    <DateView :value="value" :useLocaleFormat="true"></DateView>
  </span>
</template>

<script setup lang="ts">
import DateView from "@/components/common/views/DateView.vue";

defineProps<{
  value: string
}>()

</script>

<style scoped lang="scss">
</style>
