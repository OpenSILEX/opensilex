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
      <opensilex-GroupVariablesList v-if="loadList"
        ref="groupVariableSelection"
        :maximumSelectedRows="maximumSelectedRows"
        iconNumberOfSelectedRow="ik#ik-globe"
      ></opensilex-GroupVariablesList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class GroupVariablesModalList extends Vue {
  @Prop()
  maximumSelectedRows;

  loadList: boolean = false;

  @Ref("groupVariableSelection") readonly groupVariableSelection!: any;

  selectItem(row) {
    this.groupVariableSelection.onItemSelected(row);
  }
  unSelect(row) {
    this.groupVariableSelection.onItemUnselected(row);
  }

  show() {
      this.loadList = true;
      this.$nextTick(() => {
          let modalRef: any = this.$refs.modalRef;
          modalRef.show();
      });
  }

  hide(validate: boolean) {
    var modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.groupVariableSelection.getSelected());
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