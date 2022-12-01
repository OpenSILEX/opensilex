<template>
  <b-modal ref="modalRef" size="xxl" :static="true">
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('component.experiment.search.selectLabel') }}
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
      <opensilex-ExperimentList ref="experimentSelection" :isSelectable="true" :noActions="true"></opensilex-ExperimentList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import ExperimentList from "./ExperimentList.vue";

@Component
export default class ExperimentModalList extends ExperimentList {
  @Ref("experimentSelection") readonly experimentSelection!: any;

  selectItem(row) {
    this.experimentSelection.onItemSelected(row);
  }
  unSelect(row) {
    this.experimentSelection.onItemSelected(row);
  }

  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.experimentSelection.getSelected());
    }
  }
}
</script>

<style scoped lang="scss">
</style>
