<template>

  <b-modal ref="modalRef" size="xl" :static="true">
    
    <template v-slot:modal-title>
        <i class="ik ik-search mr-1"></i> {{ $t('component.project.filter-description') }}
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
            
          <!-- Year -->
          <opensilex-FilterField>
            <opensilex-InputForm
              :value.sync="yearFilterPattern"
              label="component.project.filter-year"
              type="text"
              placeholder="component.project.filter-year-placeholder"
            ></opensilex-InputForm>
          </opensilex-FilterField>

          <!-- Label -->
          <opensilex-FilterField>
            <opensilex-InputForm
              :value.sync="nameFilterPattern"
              label="component.project.filter-label"
              type="text"
              placeholder="component.project.filter-label-placeholder"
            ></opensilex-InputForm>
          </opensilex-FilterField>

        </template>
            
      </opensilex-SearchFilterField>

      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="loadData"
        :fields="fields"
        defaultSortBy="startDate"
        isSelectable="true"
        labelNumberOfSelectedRow="component.project.selectedLabel"
        defaultPageSize="10"
      >
        <template v-slot:cell(uri)="{data}">
          <opensilex-UriLink :uri="data.item.uri"></opensilex-UriLink>
        </template>

        <template v-slot:cell(startDate)="{data}">
          <opensilex-DateView :value="data.item.startDate"></opensilex-DateView>
        </template>

        <template v-slot:cell(endDate)="{data}">
          <opensilex-DateView :value="data.item.endDate"></opensilex-DateView>
        </template>

      </opensilex-TableAsyncView>

    </div>

  </b-modal>

</template>

<script lang="ts">
import moment from "moment";
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { ProjectGetDTO, ProjectsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib//HttpResponse";
import ProjectList from "./ProjectList.vue";

@Component
export default class ProjectModalList extends ProjectList {

  fields = [
    {
      key: "uri",
      label: "component.common.uri",
      sortable: true
    },
    {
      key: "name",
      label: "component.common.name",
      sortable: true
    },
    {
      key: "shortname",
      label: "component.project.shortname",
      sortable: true
    },
    {
      key: "startDate",
      label: "component.common.startDate",
      sortable: true
    },
    {
      key: "endDate",
      label: "component.common.endDate",
      sortable: true
    },
    {
      key: "financialFunding",
      label: "component.project.financialFunding",
      sortable: true
    }
  ];

  resetFilters() {

  }

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
