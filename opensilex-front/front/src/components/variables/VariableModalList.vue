<template>
  <b-modal ref="modalRef" size="xl" :static="true">
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
      <opensilex-VariableListWithoutActionButton
        ref="variableSelection"
        :isSelectable="true"
        :noActions="true"
        :maximumSelectedRows="maximumSelectedRows"
        iconNumberOfSelectedRow="ik#ik-globe"
      ></opensilex-VariableListWithoutActionButton>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VariableList from "./VariableList.vue";

@Component
export default class VariableModalList extends Vue {
  @Prop()
  maximumSelectedRows;

  @Ref("variableSelection") readonly variableSelection!: any;

  unSelect(row) {
    this.variableSelection.onItemUnselected(row);
  }

  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.variableSelection.getSelected());
    }
  }
}
</script>

<style scoped >
@media (min-width: 576px) {
  div >>> .modal-xl {
    max-width: 800px;
  }
}

@media (min-width: 1200px) {
  div >>> .modal-xl {
    max-width: 1140px;
  }
}
</style>
