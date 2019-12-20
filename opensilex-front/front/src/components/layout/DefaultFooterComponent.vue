<template>
  <div class="w-100 clearfix">
      <span class="text-center text-sm-left d-md-inline-block">{{ $t('component.footer.copyright', { version: '1.0.0', date: '19/12/2019' }) }}</span>
  </div>
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';

@Component
export default class DefaultFooterComponent extends Vue {
}
</script>

<style scoped lang="scss">
</style>
