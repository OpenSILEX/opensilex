<template>
  <div id="footer">
      <span class="text-center">© OpenSILEX - Siduri v.1.0.0</span>
  </div>
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';
import DefaultFooterComponent from '../../../../../opensilex-front/front/src/components/layout/DefaultFooterComponent.vue';

@Component
export default class SiduriFooterComponent extends DefaultFooterComponent {
}
</script>

<style scoped lang="scss">

</style>
