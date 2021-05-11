<template>
  <b-modal ref="modalRef" size="xl" :static="true">
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('GermplasmList.selectLabel') }}
    </template>

    <template v-slot:modal-footer>
      <button
        type="button"
        class="btn btn-secondary"
        v-on:click="hide(false)"
      >{{ $t('component.common.close') }}</button>
      <button
        type="button"
        class="btn btn-primary"
        v-on:click="hide(true)"
      >{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div class="card">
      <opensilex-GermplasmList ref="germplasmSelection" :isSelectable="true" :noActions="true"></opensilex-GermplasmList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import GermplasmList from "./GermplasmList.vue";

@Component
export default class GermplasmModalList extends GermplasmList {
  @Ref("germplasmSelection") readonly germplasmSelection!: any;

  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.germplasmSelection.getSelected());
    }
  }
}
</script>

<style scoped lang="scss">
</style>
