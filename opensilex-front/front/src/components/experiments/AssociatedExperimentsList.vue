<template>
  <opensilex-Card label="component.project.experiments" icon="ik#ik-layers">
    
    <template v-slot:body>
      <b-form-group
        v-if="filter != null"> 
        <opensilex-StringFilter          
          :filter.sync="filter"
          @update="updateFilters()"
          placeholder="AssociatedExperimentsList.experimentNameFilter"
        ></opensilex-StringFilter>
      </b-form-group>
      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchMethod"
        :fields="fields"
        defaultSortBy="name"
        :defaultPageSize="5"
      >
        <template v-slot:cell(name)="{data}">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.uri)}"
          ></opensilex-UriLink>
        </template>
        <template v-slot:cell(start_date)="{data}">
          <opensilex-DateView :value="data.item.start_date"></opensilex-DateView>
        </template>
        <template v-slot:cell(end_date)="{data}">
          <opensilex-DateView :value="data.item.end_date"></opensilex-DateView>
        </template>
        <template v-slot:cell(description)="{data}">
          <span>{{textReduce(data.item.description)}}</span>
        </template>
        <template v-slot:cell(state)="{data}">
          <i
            v-if="!isEnded(data.item)"
            class="ik ik-activity badge-icon badge-info-opensilex"
            :title="$t('component.project.common.status.in-progress')"
          ></i>
          <i
            v-else
            class="ik ik-archive badge-icon badge-light"
            :title="$t('component.project.common.status.finished')"
          ></i>
        </template>
      </opensilex-TableAsyncView>
    </template>
  </opensilex-Card>
</template>

<script lang="ts">
import { Component, Ref, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment";

@Component
export default class AssociatedExperimentsList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  @Ref("tableRef") readonly tableRef!: any;

  @Prop()
  searchMethod;

  @PropSync("nameFilter")
  filter


  fields = [
    {
      key: "name",
      label: "component.common.name"
    },
    {
      key: "description",
      label: "component.common.description"
    },
    {
      key: "start_date",
      label: "component.common.startDate",
      sortable: true
    },
    {
      key: "end_date",
      label: "component.common.endDate",
      sortable: true
    },
    {
      key: "state",
      label: "component.common.state"
    }
  ];

  isEnded(experiment) {
    if (experiment.end_date) {
      return moment(experiment.end_date, "YYYY-MM-DD").diff(moment()) < 0;
    }

    return false;
  }

  textReduce(text) {
    if (text !== null && text.length > 60) {
      var shortname = text.substring(0, 60) + " ...";
      return text.substring(0, 60) + " ...";
    } else {
      return text;
    }
  }

  updateFilters() {
    this.tableRef.refresh();
  }

}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
  AssociatedExperimentsList:
    experimentNameFilter: Search on experiment name

fr:
  AssociatedExperimentsList:
    experimentNameFilter: Chercher sur le nom de l'expérimentation

</i18n>