<template>
  <div>
    <opensilex-Card v-if="selected" :label="$t('GeneticResourceGroupView.type')" icon="fa#seedling">
      <template v-slot:body>


          <opensilex-TableAsyncView
              ref="tableRef"
              :searchMethod="searchGeneticResourceGroupContent"
              defaultPageSize=5
              :fields="relationsFields"
              :isSelectable="false"
              defaultSortBy="name"
          >

          <template v-slot:cell(name)="{data}">
            <opensilex-UriLink
                :uri="data.item.uri"
                :value="data.item.name"
                :to="{path: '/geneticResource/details/'+ encodeURIComponent(data.item.uri)}">
            </opensilex-UriLink>
          </template>

        </opensilex-TableAsyncView>


      </template>
    </opensilex-Card>
  </div>
</template>

<script lang="ts">

import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import TableAsyncView from "../common/views/TableAsyncView.vue";
import {GeneticResourceGetAllDTO} from "opensilex-core/model/geneticResourceGetAllDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GeneticResourceGroupContentList extends Vue{
  @Prop()
  selected? : string;

  @Prop()
  relationsFields? : any;

  @Ref("tableRef") readonly tableRef!: TableAsyncView<GeneticResourceGetAllDTO>;

  service: GeneticResourceService;
  $opensilex: OpenSilexVuePlugin;

  created() {
    this.service = this.$opensilex.getService("opensilex.GeneticResourceService")
  }

  refresh() {
    this.tableRef.refresh();
  }

  searchGeneticResourceGroupContent(options) {
    return this.service.getGeneticResourceGroupContent(this.selected, options.orderBy, options.currentPage, options.pageSize);
  }

}
</script>