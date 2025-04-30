<template>
  <div>
    <h2>VariablesView</h2>
    <p><strong>Route actuelle :</strong> {{ currentPath }}</p>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, watch, onMounted } from "vue";
import { useRoute } from "vue-router";

export default defineComponent({
  name: "VariablesView",
  setup() {
    const route = useRoute();
    const currentPath = ref(route.fullPath);


    onMounted(() => {
      console.log("[VariablesView] Composant monté !");
    });

    return {
      currentPath,
    };
  },
});
</script>

<style scoped>
h2 {
  margin-bottom: 0.5rem;
}
</style>
