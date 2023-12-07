<template>
  <b-dropdown
      class="v-step-skos-selector"
      dropdown
      boundary="window"
      :small="true"
      :text="dropdownLabel"
  >
    <b-dropdown-item
        v-for="(relation) in skosRelationOptions"
        :key="relation.id"
        class="btn-dropdown"
        @click="onClick(relation)"
    >
      {{ relation.label }}
    </b-dropdown-item>
  </b-dropdown>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {SelectableItem} from "../../forms/SelectForm.vue";
import SUPPORTED_SKOS_RELATIONS from "../../../../models/SkosRelations";
import {Prop, PropSync} from "vue-property-decorator";

@Component({})
export default class SkosSelector extends Vue {
  //region Props
  @Prop({default: false})
  private readonly labelAsCurrentSelectedRelation: boolean;
  @PropSync("selectedRelation")
  private selected?: string;
  //endregion

  //region Data
  private skosRelationOptions: Array<SelectableItem> = [];
  //endregion

  //region Computed
  private get dropdownLabel(): string {
    if (this.labelAsCurrentSelectedRelation && this.selected) {
      const selectedItem = this.skosRelationOptions.find(item => item.id === this.selected);
      return this.$t(selectedItem.label).toString();
    }
    return this.$t("SkosSelector.label").toString();
  }
  //endregion

  //region Hooks
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

  mounted() {
    if (this.selected) {
      this.emitSelected();
    }
  }
  //endregion

  //region Events
  /**
   *
   * Not a fan of this because as a PropSync, `selected` already triggers events. If I have time I will remove
   * this event and fix the components that depend on it.
   *
   * @private
   */
  private emitSelected() {
    this.$emit("selected", this.selected);
  }
  //endregion

  //region Event handlers
  private onClick(relation: SelectableItem) {
    this.selected = relation.id;
    this.emitSelected();
  }
  //endregion
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