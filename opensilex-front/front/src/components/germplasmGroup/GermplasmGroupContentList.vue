<template>
  <div>
    <opensilex-Card v-if="selected" :label="$t('GermplasmGroupView.type')" icon="fa#seedling">
      <template v-slot:body>


          <opensilex-TableAsyncView
              ref="tableRef"
              :searchMethod="searchGermplasmGroupContent"
              defaultPageSize=5
              :fields="relationsFields"
              :isSelectable="false"
              defaultSortBy="name"
          >

          <template v-slot:cell(name)="{data}">
            <opensilex-UriLink
                :uri="data.item.uri"
                :value="data.item.name"
                :to="{path: '/germplasm/details/'+ encodeURIComponent(data.item.uri)}">
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
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import TableAsyncView from "../common/views/TableAsyncView.vue";
import {GermplasmGetAllDTO} from "opensilex-core/model/germplasmGetAllDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class GermplasmGroupContentList extends Vue{
  @Prop()
  selected? : string;

  @Prop()
  relationsFields? : any;

  @Ref("tableRef") readonly tableRef!: TableAsyncView<GermplasmGetAllDTO>;

  service: GermplasmService;
  $opensilex: OpenSilexVuePlugin;

  created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService")
  }

  refresh() {
    this.tableRef.refresh();
  }

  searchGermplasmGroupContent(options) {
    return this.service.getGermplasmGroupContent(this.selected, options.orderBy, options.currentPage, options.pageSize);
  }

}
</script>