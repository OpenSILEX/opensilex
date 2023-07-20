<template>
  <div class="card"
    @click="$emit('click')">
    <div v-if="!noHeader" class="card-header">
      <h3 class="capitalize-first-letter">
        <opensilex-Icon v-if="icon" :icon="icon" class="icon-title" />
        {{ $t(label) }}
      </h3>
      <slot name="header"></slot>
      <div class="card-header-right">
        <slot name="rightHeader"></slot>
      </div>
    </div>
    <div class="card-body">
      <slot name="body"></slot>
    </div>
    <div v-if="!noFooter" class="card-footer text-center">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DescriptionCard extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;

  @Prop()
  noFooter: boolean;
  @Prop()
  noHeader: boolean;
  @Prop()
  label: string;
  @Prop()
  icon: string;
}
</script>

<style scoped lang="scss">

.capitalize-first-letter {
  width: 100%;
}

</style>
