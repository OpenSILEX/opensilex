<template>
  <b-modal ref="modalRef" size="xl" :static="true" @shown="$emit('shown')">
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
      <opensilex-ScientificObjectList 
        ref="soList"
        :isSelectable="true"
        :noActions="true"
        :pageSize="10"
        :searchFilter.sync="filter"
        :noUpdateURL="true">
      </opensilex-ScientificObjectList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, PropSync } from "vue-property-decorator";
import ScientificObjectList from "./ScientificObjectList.vue";

@Component
export default class ScientificObjectModalList extends ScientificObjectList {

    
  @Ref("soList") readonly soList!: any;

  unSelect(row) {
    this.soList.onItemUnselected(row);
  }
  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      let select = this.soList.getSelected();
      this.$emit("onValidate", this.soList.getSelected());
    }
  }

  refresh() {
    this.soList.refresh();
  }
}
</script>

<style scoped lang="scss">
</style>
