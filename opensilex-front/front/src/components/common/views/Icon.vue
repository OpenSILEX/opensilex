<template>
  <span>
    <font-awesome-icon v-if="iconType === 'fa'" :icon="faClass" :size="size" />
    <i v-else-if="iconType === 'bi'" :class="bootstrapClasses"></i>
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  icon?: string;
  size?: string;
}>();

const iconType = computed(() => {
  const type = props.icon?.split("#")[0];
  return ["fa", "bi"].includes(type || "") ? type : "bi";
});

const iconName = computed(() => props.icon?.split("#")[1] || "bi-folder");

const faClass = computed(() => iconName.value);
const bootstrapClasses = computed(() => {
  const name = iconName.value.startsWith("bi-") ? iconName.value.slice(3) : iconName.value;
  return `bi bi-${name}`;
});

</script>

<style scoped lang="scss">
span {
  margin: 0;
  padding: 0;
  display: inline;
  border: none;
}
</style>
