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
      <opensilex-VariableList
        ref="variableSelection"
        :noActions="true"
        :pageSize="5"
        :maximumSelectedRows="maximumSelectedRows"
        :withAssociatedData="withAssociatedData"
        :experiment="experiment"
        :objects="objects"
        :devices="devices"
        @select="$emit('select', $event)"
        @unselect="$emit('unselect', $event)"
        @selectall="$emit('selectall', $event)"
      ></opensilex-VariableList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VariableModalList extends Vue {

  @Ref("variableSelection") readonly variableSelection!: any;
  @Ref("modalRef") readonly modalRef!: any;

  @Prop()
  maximumSelectedRows;

  @Prop()
  withAssociatedData;

  @Prop()
  experiment;

  @Prop()
  objects;

  @Prop()
  devices;

  unSelect(row) {
    this.variableSelection.onItemUnselected(row);
  }

  show() {
    this.modalRef.show();
  }

  hide(validate: boolean) {
    this.modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.variableSelection.getSelected());
    } else {
      this.$emit("onClose");
    }
  }

  refresh() {
    this.variableSelection.refresh();
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
