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

      <div class="card-vertical-group">

        <div class="card">

          <div class="card-body row">

            <!-- Year -->
            <div class="filter-group col col-xl-3 col-sm-6 col-12">
              <opensilex-InputForm
                :value.sync="yearFilterPattern"
                label="component.project.filter-year"
                type="text"
                placeholder="component.project.filter-year-placeholder"
              ></opensilex-InputForm>
            </div>

            <!-- Label -->
            <div class="filter-group col col-xl-3 col-sm-6 col-12">
              <opensilex-InputForm
                :value.sync="nameFilterPattern"
                label="component.project.filter-label"
                type="text"
                placeholder="component.project.filter-label-placeholder"
              ></opensilex-InputForm>
            </div>

          </div>

          <!-- Form actions -->
          <div class="card-footer text-right">
            <button type="button" class="btn btn-primary float-right mb-2 mr-2" @click="refresh">
              <i class="ik ik-search"></i>{{$t('component.germplasm.filter.search')}}
            </button>
            <button type="button" class="btn btn-light float-right mb-2 mr-2" @click="resetFilters">
              <i class="ik ik-x"></i>{{$t('component.germplasm.filter.reset')}}
            </button>          
          </div>

        </div>

      </div>

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
      key: "label",
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
      key: "hasFinancialFunding",
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
