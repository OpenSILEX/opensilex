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
        class="btn greenThemeColor"
        v-on:click="hide(true)"
      >{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div>
      <opensilex-ProjectList 
        ref="projectSelection"
        :isSelectable="true"
        :noActions="true"
        :pageSize="5"
        :noUpdateURL="true"
        @select="$emit('select', $event)"
        @unselect="$emit('unselect', $event)"
        @selectall="$emit('selectall', $event)"
        ></opensilex-ProjectList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref, PropSync} from "vue-property-decorator";

@Component
export default class ProjectModalList extends Vue {
  @Ref("projectSelection") readonly projectSelection!: any;

  selectItem(row) {
    this.projectSelection.onItemSelected(row);
  }
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
   refresh() {
    this.projectSelection.refresh();
  }
}
</script>

<style scoped lang="scss">
</style>
