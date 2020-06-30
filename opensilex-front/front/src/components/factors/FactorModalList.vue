<template>

  <b-modal ref="modalRef" size="xl" :static="true">
    
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i> {{ $t('GermplasmList.selectLabel') }}
    </template>

    <template v-slot:modal-footer>
      <button type="button" class="btn btn-secondary" v-on:click="hide(false)">{{ $t('component.common.close') }}</button>
      <button type="button" class="btn btn-primary" v-on:click="hide(true)">{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div class="card">

      <opensilex-SearchFilterField
        @search="refresh()"
        @clear="resetFilters()"
      >
          
        <template v-slot:filters>

          <!-- Name -->
          <opensilex-FilterField>
              <opensilex-StringFilter
                label="component.factor.filter.name"
                :filter.sync="filter.name"
                placeholder="component.factor.filter.name-placeholder"
              ></opensilex-StringFilter>
          </opensilex-FilterField>
        
        </template>

      </opensilex-SearchFilterField>

      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchFactors"
        :fields="fields"
        defaultSortBy="name"
        isSelectable="true"
        defaultPageSize="10"
        labelNumberOfSelectedRow="GermplasmList.selected"
        iconNumberOfSelectedRow="ik#ik-feather"
      >

        <template v-slot:cell(uri)="{data}">
            <opensilex-UriLink
            :uri="data.item.uri"
            ></opensilex-UriLink>
        </template>

      </opensilex-TableAsyncView>

    </div>

    </b-modal>

</template>

<script lang="ts">

import { Component, Ref, Prop } from "vue-property-decorator";
import FactorList from './FactorList.vue';

@Component
export default class FactorModalList extends FactorList {

  fields = [
    {
      key: "uri",
      label: "component.factor.uri",
      sortable: true
    },
    {
      key: "name",
      label: "component.factor.name",
      sortable: true
    },
    {
      key: "comment",
      label: "component.factor.comment",
      sortable: false
    }
  ];

  show() {
      let modalRef: any = this.$refs.modalRef;
      modalRef.show();
  }

  hide(validate: boolean) {
      let modalRef: any = this.$refs.modalRef;
      modalRef.hide();

      if(validate) {
          this.$emit("onValidate", this.tableRef.getSelected());
      }
  }

}
</script>

<style scoped lang="scss">

</style>
