<template>
  <b-dropdown
      class="v-step-skos-selector"
      :right="right"
      boundary="window"
      :small="true"
      :text="dropdownLabel"
  >
    <b-dropdown-item
        v-for="(relation) in skosRelationOptions"
        :key="'key-dropdown-' + relation.id"
        class="btn-dropdown"
        v-b-tooltip.hover="{
          placement: 'right',
          title: relation.title
        }"
        @click="onClick(relation)"
    >
      {{ relation.label }}
    </b-dropdown-item>
  </b-dropdown>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {SelectableItem} from "../../forms/FormSelector.vue";
import SUPPORTED_SKOS_RELATIONS from "../../../../models/SkosRelations";
import {Prop, PropSync} from "vue-property-decorator";

@Component({})
export default class SkosSelector extends Vue {
  //#region Props
  @Prop({default: false})
  private readonly labelAsCurrentSelectedRelation: boolean;
  @Prop({default: false})
  private readonly right: boolean;
  @PropSync("selectedRelation")
  private selected?: string;
  //#endregion

  //#region Data
  private skosRelationOptions: Array<SelectableItem> = [];
  //#endregion

  //#region Computed
  private get dropdownLabel(): string {
    if (this.labelAsCurrentSelectedRelation && this.selected) {
      const selectedItem = this.skosRelationOptions.find(item => item.id === this.selected);
      return this.$t(selectedItem.label).toString();
    }
    return this.$t("SkosSelector.label").toString();
  }
  //#endregion

  //#region Hooks
  created() {
    const skosRelationOptions: Array<SelectableItem> = [];
    for (const skosRelation of SUPPORTED_SKOS_RELATIONS) {
      skosRelationOptions.push({
        id: skosRelation.dtoKey,
        label: this.$t(skosRelation.label).toString(),
        title: this.$t(skosRelation.description).toString()
      });
    }
    this.skosRelationOptions = skosRelationOptions;
  }
  //#endregion

  //#region Event handlers
  private onClick(relation: SelectableItem) {
    this.selected = relation.id;
  }
  //#endregion
}
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  SkosSelector:
    label: Map term as
fr:
  SkosSelector:
    label: Choisir une relation
</i18n>