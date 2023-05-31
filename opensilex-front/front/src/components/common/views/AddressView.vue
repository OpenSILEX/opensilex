<template>
  <div>
    <!-- Address -->
    <opensilex-StringView
        v-if="address"
        :value="address.readableAddress"
        label="AddressView.address"
    ></opensilex-StringView>
  </div>
</template>

<script lang="ts">

import Vue from "vue";
import {Component, Prop} from "vue-property-decorator";

@Component
export default class AddressView extends Vue {
  @Prop()
  address: {
    readableAddress: string
  };
}
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