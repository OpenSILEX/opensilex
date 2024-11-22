<template>
  <b-table
    class="v-step-skos-relation-table"
    striped
    hover
    small
    responsive
    sort-icon-left
    bordered
    :items="uriRelationList"
    :fields="fields"
  >
    <template v-slot:head(relation)="head">{{ $t(head.label) }}</template>
    <template v-slot:cell(relation)="cell">
      <opensilex-SkosSelector
          :labelAsCurrentSelectedRelation="true"
          :selectedRelation.sync="cell.item.relationDtoKey"
          @update:selectedRelation="onSelected"
      ></opensilex-SkosSelector>
    </template>
    <template v-slot:head(relationURI)="head">{{ $t(head.label) }}</template>
    <template v-slot:cell(relationURI)="cell">
      <a :href="cell.value" target="_blank">{{ cell.item.uri }}</a>
    </template>
    <template v-slot:head(actions)="head">{{ $t(head.label) }}</template>
    <template v-slot:cell(actions)="cell">
      <div class="text-center">
        <b-button-group size="md">
          <b-button
              size="md"
              @click="removeRelation(cell.item.relationDtoKey, cell.item.uri)"
              variant="danger"
          >
            <opensilex-Icon icon="fa#trash-alt"/>
          </b-button>
        </b-button-group>
      </div>
    </template>
  </b-table>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {PropSync} from "vue-property-decorator";
import {BvTableFieldArray} from "bootstrap-vue";
import {UriSkosRelation} from "../../../../models/SkosRelations";

@Component({})
export default class SkosRelationTable extends Vue {
  //#region Props
  @PropSync("uriRelations")
  private uriRelationList: Array<UriSkosRelation>;
  //#endregion

  //#region Data
  private readonly fields: BvTableFieldArray = [
    {
      key: "relation",
      label: "component.skos.relation",
      sortable: true
    },
    {
      key: "relationURI",
      label: "component.skos.uri",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];
  //#endregion

  //#region Private methods
  private findRelationUriIndex(dtoKey: string, relationUri: string): number {
    return this.uriRelationList.findIndex(uriRelation => uriRelation.relationDtoKey === dtoKey
      && uriRelation.uri === relationUri);
  }

  private removeRelation(dtoKey: string, relationUri: string) {
    const index = this.findRelationUriIndex(dtoKey, relationUri);
    console.debug("Remove relation", dtoKey, relationUri, index);
    if (index >= 0) {
      this.uriRelationList.splice(index, 1);
      this.emitUpdateUriRelations();
    }
  }
  //#endregion

  //#region Events
  /**
   * Not exactly sure why, maybe because it's a PropSync that is also an array, but updates don't trigger
   * when using `splice` or when setting a property of an object inside `uriRelationList`. So this method exist
   * for triggering reactivity. Maybe investigate and find a better way to do that.
   *
   * @private
   */
  private emitUpdateUriRelations() {
    this.$emit("update:uriRelations", this.uriRelationList);
  }
  //#endregion

  //#region Event handlers
  private onSelected() {
    this.emitUpdateUriRelations();
  }
  //#endregion
}
</script>

<style scoped lang="scss">

</style>