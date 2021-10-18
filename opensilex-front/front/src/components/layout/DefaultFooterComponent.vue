<template>
  <div class="footer ">
    <div class="w-100 clearfix">
      <span
        class="text-center text-sm-left d-md-inline-block"
      >{{ $t('component.footer.copyright', { version: $opensilex.version}) }}</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";

import { VersionInfoDTO } from "opensilex-core/index";

@Component
export default class DefaultFooterComponent extends Vue {
  $opensilex: any;
  $store: any;

  versionInfo: VersionInfoDTO;

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
  }

}
</script>

<style scoped lang="scss">
</style>
