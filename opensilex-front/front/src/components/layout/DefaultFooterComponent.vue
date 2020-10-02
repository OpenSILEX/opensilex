<template>
  <div class="footer ">
    <div class="w-100 clearfix">
      <span
        class="text-center text-sm-left d-md-inline-block"
      >{{ $t('component.footer.copyright', { version: release.version, date: release.date }) }}</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DefaultFooterComponent extends Vue {

  get release() {
    return this.$store.state.release;
  }

}
</script>

<style scoped lang="scss">
</style>
