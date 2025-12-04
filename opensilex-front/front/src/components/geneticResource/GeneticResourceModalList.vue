<template>
  <b-modal ref="modalRef" size="xl" :static="true" @hide='$emit("hide")'>
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i>
      {{ $t('GeneticResourceList.selectLabel') }}
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

    <div class="card">
      <opensilex-GeneticResourceList
          ref="geneticResourceSelection"
          :isSelectable="true"
          :noActions="true"
          :pageSize="5"
          :noUpdateURL="true"
          :experimentUri="experiment"
          @select="$emit('select', $event)"
          @unselect="$emit('unselect', $event)"
          @selectall="$emit('selectall', $event)"
      ></opensilex-GeneticResourceList>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import GeneticResourceList from "./GeneticResourceList.vue";

@Component
export default class GeneticResourceModalList extends GeneticResourceList {
  @Ref("geneticResourceSelection") readonly geneticResourceSelection!: any;

  /**
   * Set an experiment uri, in this case we don't show experiment filter and show only geneticResources of this experiment
   */
  @Prop()
  experiment: string;

  selectItem(row) {
      this.geneticResourceSelection.onItemSelected(row);
  }
  unSelect(row) {
    this.geneticResourceSelection.onItemUnselected(row);
  }

  setInitiallySelectedItems(initiallySelectedItems:Array<any>){
    this.geneticResourceSelection.setInitiallySelectedItems(initiallySelectedItems);
  }

  show() {
    let modalRef: any = this.$refs.modalRef;
    this.$emit("shown");
    modalRef.show();
  }

  refresh() {
    this.geneticResourceSelection.refresh();
  }

  hide(validate: boolean) {
    let modalRef: any = this.$refs.modalRef;
    modalRef.hide();
    if (validate) {
      this.$emit("onValidate", this.geneticResourceSelection.getSelected());
    }
  }
}
</script>

<style scoped lang="scss">
</style>
