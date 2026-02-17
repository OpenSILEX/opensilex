<template>
  <div>
    <!-- Address -->
    <opensilex-StringView
      v-if="address"
      :value="address.readableAddress"
      :label="t('AddressView.address')"
    />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

type Address = {
  readableAddress: string;
};

const { t } = useI18n()

defineProps<{
  address?: Address | null;
}>();
</script>

<i18n>
en:
  AddressView:
    address: Address
fr:
  AddressView:
    address: Adresse
</i18n>

<style scoped>
</style>
