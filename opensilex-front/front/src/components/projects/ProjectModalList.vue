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
      <opensilex-ProjectList 
        ref="projectSelection"
        :isSelectable="true"
        :maximumSelectedRows="maximumSelectedRows"
        :noActions="true"
        :noUpdateURL="true"
        ></opensilex-ProjectList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import ProjectList from "./ProjectList.vue";

@Component
export default class ProjectModalList extends ProjectList {
  @Ref("projectSelection") readonly projectSelection!: any;

  unSelect(row) {
    this.projectSelection.onItemUnselected(row);
  }
  show() {
    let modalRef: any = this.$refs.modalRef;
    modalRef.show();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();

    if (validate) {
      this.$emit("onValidate", this.projectSelection.getSelected());
    }
  }
}
</script>

<style scoped lang="scss">
</style>
