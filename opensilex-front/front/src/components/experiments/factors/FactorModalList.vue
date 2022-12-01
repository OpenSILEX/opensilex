<template>
  <b-modal ref="modalRef" size="xl" :static="true">
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('FactorList.selectLabel') }}
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
      <opensilex-FactorList ref="factorSelection" :isSelectable="true" :noActions="true"></opensilex-FactorList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import FactorList from "./FactorList.vue";

@Component
export default class FactorModalList extends FactorList {
  @Ref("factorSelection") readonly factorSelection!: any;

  selectItem(row) {
    this.factorSelection.onItemSelected(row);
  }
  unSelect(row) {
    this.factorSelection.onItemSelected(row);
  }

  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.factorSelection.getSelected());
    }
  }
}
</script>

<style scoped lang="scss">
</style>
