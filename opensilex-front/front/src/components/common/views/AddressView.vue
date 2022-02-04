<template>
  <div>
    <!-- Address -->
    <opensilex-StringView
        v-if="address"
        :value="address.readableAddress"
        label="AddressView.address"
    ></opensilex-StringView>

    <!-- Geometry -->
    <opensilex-GeometryCopy
        v-if="geometry"
        :value="geometry"
    ></opensilex-GeometryCopy>

    <!-- No geometry warning -->
    <b-alert
        v-if="!geometry && noGeometryLabel"
        show
        variant="warning"
    >
      {{ $t(noGeometryLabel) }}
    </b-alert>
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

  @Prop()
  geometry;

  @Prop()
  noGeometryLabel: string;
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