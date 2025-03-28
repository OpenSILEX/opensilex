<template>
  <div class="container-fluid">
    <opensilex-Dashboard></opensilex-Dashboard>
    <!-- <div>Import composant HomeView </div> -->
  </div>
</template>

<script lang="ts">
import { defineComponent, computed } from "vue";
import { useStore } from "vuex";

export default defineComponent({
  name: "HomeView",
  setup() {
    const store = useStore();
    const user = computed(() => store.state.user);

    return {
      user,
    };
  },
});
</script>

<style scoped lang="scss">
</style>
