<template>
  <div >
<!--      <template>-->
<!--        <opensilex-TableAsyncView-->
<!--            ref="tableRef"-->
<!--            :fields="fields"-->
<!--            :searchMethod="LabelDtos"-->
<!--            defaultSortBy="prefLabel"-->
<!--            iconNumberOfSelectedRow="fa#vials"-->
<!--            class="modalLabelsList"-->
<!--        >-->
<!--          <template v-slot:cell(prefLabel)="labelDTOList">-->
<!--            {{ labelDTOList.item.prefLabel }}-->
<!--          </template>-->
<!--          <template v-slot:cell(definition)="labelDTOList">-->
<!--            {{ labelDTOList.item.definition }}-->
<!--          </template>-->
<!--&lt;!&ndash;          <template v-slot:cell(lang)="labelDTOList">&ndash;&gt;-->
<!--&lt;!&ndash;            {{ labelDTOList.item.lang }}&ndash;&gt;-->
<!--&lt;!&ndash;          </template>&ndash;&gt;-->
<!--        </opensilex-TableAsyncView>-->
<!--      </template>-->
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref, Vue} from "vue-property-decorator";
import {LabelDTO} from "opensilex-core/model/labelDTO";

@Component
export default class LabelsList extends Vue {

  // @Ref("tableRef") readonly tableRef!: any;
  //
  // @Prop()
  // labelDTOs: Array<LabelDTO>;
  //
  // init(){
  //   this.labelDTOs=[];
  // }
  //
  // getLabelDTOsFromEntityForm(labelDTOs: Array<LabelDTO>){
  //
  //
  //   this.labelDTOs = labelDTOs;
  //
  // }
  //
  // get LabelDtos(){
  //   console.log("labelsDtos - labelList" + this.labelDTOs)
  //   return this.labelDTOs
  // }
  //
  // get fields() {
  //   return [
  //     {
  //       key: "prefLabel",
  //       label: this.$t("component.common.prefLabel"),
  //       sortable: true,
  //     },
  //     // {
  //     //   key: "altLabels",
  //     //   label: this.$t("component.common.altLabels"),
  //     //   sortable: false,
  //     // },
  //     {
  //       key: "definition",
  //       label: this.$t("component.common.definition"),
  //       sortable: false,
  //     },
  //     // {
  //     //   key: "lang",
  //     //   label: this.$t("component.common.lang"),
  //     //   sortable: false,
  //     // },
  //   ];
  // }
}
</script>

<style scoped lang="scss">
.modalLabelsList {
  overflow: hidden;
}

.variablesCheckboxMarginHighSize {
  margin-left: 15px;
}

@media (min-width: 200px) and (max-width: 1199px) {
  .lowSize {
    margin-left: 15px;
  }
}
</style>
