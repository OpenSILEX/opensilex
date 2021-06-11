<template>
  <b-modal ref="modalRef" size="xl" :static="true">
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('component.project.filter-description') }}
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
      <opensilex-ProvenanceList 
        ref="provenancesSelection"
        :isSelectable="true"
        :noActions="true">
      </opensilex-ProvenanceList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import ProvenanceList from "./ProvenanceList.vue";

@Component
export default class ProvenanceModalList extends ProvenanceList {
    
  @Ref("provenancesSelection") readonly provenancesSelection!: any;

  unSelect(row) {
    this.provenancesSelection.onItemUnselected(row);
  }
  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      let select = this.provenancesSelection.getSelected();
      this.$emit("onValidate", this.provenancesSelection.getSelected());
    }
  }
}
</script>

<style scoped lang="scss">
</style>