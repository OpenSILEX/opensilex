<template>
  <opensilex-Card label="AssociatedExperimentsList.relatedExperiments" icon="ik#ik-layers">
    
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
        <template v-slot:cell(species)="{data}">
          <span class="species-list" v-if="data.item.species.length > 0">
            <span :key="index" v-for="(uri, index) in data.item.species">
              <span :title="uri">{{ getSpeciesName(uri) }}</span>
              <span v-if="index + 1 < data.item.species.length">, </span>
            </span>
          </span>
          <span v-else></span>
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
          <i
              v-if="data.item.is_public"
              class="ik ik-users badge-icon badge-info"
              :title="$t('component.experiment.common.status.public')"
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
import {SpeciesService} from "opensilex-core/api/species.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import { SpeciesDTO } from 'opensilex-core/index';

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
      key: "species",
      label: "component.common.species"
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

  species = [];
  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  loadSpecies() {
    let service: SpeciesService = this.$opensilex.getService(
        "opensilex.SpeciesService"
    );
    service
        .getAllSpecies()
        .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
          this.species = [];
          for (let i = 0; i < http.response.result.length; i++) {
            this.speciesByUri.set(
                http.response.result[i].uri,
                http.response.result[i]
            );
            this.species.push({
              id: http.response.result[i].uri,
              label: http.response.result[i].name,
            });
          }
          // force refresh of the table
          this.tableRef.refresh();
        })
        .catch(this.$opensilex.errorHandler);
  }

  isEnded(experiment) {
    if (experiment.end_date) {
      return moment(experiment.end_date, "YYYY-MM-DD").diff(moment()) < 0;
    }

    return false;
  }

  getSpeciesName(uri: String): String {
    if (this.speciesByUri.has(uri)) {
      return this.speciesByUri.get(uri).name;
    }
    return "";
  }

  updateFilters() {
    this.tableRef.refresh();
  }

  created() {
    this.loadSpecies();
  }

}
</script>


<style scoped lang="scss">
.species-list {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 150px;
}
</style>

<i18n>
en:
  AssociatedExperimentsList:
    experimentNameFilter: Search on experiment name
    relatedExperiments: Related Experiments

fr:
  AssociatedExperimentsList:
    experimentNameFilter: Chercher sur le nom de l'expérimentation
    relatedExperiments: Expérimentations Liées

</i18n>