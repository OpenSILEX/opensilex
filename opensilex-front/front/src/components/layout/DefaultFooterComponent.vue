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
import Vue, { defineComponent } from "vue";
// @ts-ignore
import { versionInfoDTO } from "opensilex-core/index";

export default defineComponent({
  name: 'defaultFooterComponent',
    data() {
        const versionInfo: versionInfoDTO = undefined;
        const $store: any = undefined;
        const $opensilex: any = undefined;

        return {
            $opensilex,
            $store,
            versionInfo
        };
    },
    created() {
        this.versionInfo = this.$opensilex.versionInfo;
    }
})

</script>

<style scoped lang="scss">
</style>
