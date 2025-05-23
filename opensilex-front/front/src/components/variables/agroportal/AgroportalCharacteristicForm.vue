<template>
  <div>
    <h5>AgroportalCharacteristicForm </h5>
  </div>
</template>

<script setup lang="ts">
import { computed }  from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';

const props = defineProps<{
}>();

const store = useStore();
const { t } = useI18n();

</script>

<style scoped lang="scss">
</style>
