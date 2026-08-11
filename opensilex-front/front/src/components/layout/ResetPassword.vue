<template>
  <div class="container-fluid">
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from "vue";
import { useRoute, useRouter } from "vue-router";
import { renewPassword } from "opensilex-security";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const route = useRoute();
const router = useRouter();

const passwordToken = ref<string>("");
const password = ref<string>("");
const confirmation = ref<string>("");
const badToken = ref<boolean>(false);

onMounted(async () => {
  const token = route.params.uri ? decodeURIComponent(route.params.uri as string) : "";
  if (!token) {
    router.push({ path: "/" });
    return;
  }
  passwordToken.value = token;

  const { error } = await renewPassword({
    query: {
      passwordToken: token,
      checkOnly: true
    }
  });

  if (error) {
    badToken.value = true;
  }
});
</script>

<style scoped lang="scss">
</style>