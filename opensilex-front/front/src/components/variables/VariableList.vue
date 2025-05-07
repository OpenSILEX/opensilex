<template>
  <div>
    <h2>VariablesList</h2>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';

const emit = defineEmits(['ready']);

onMounted(() => {
  console.log("[VariableList] Composant monté !");
  emit('ready');
});
</script>


<style scoped>
</style>
