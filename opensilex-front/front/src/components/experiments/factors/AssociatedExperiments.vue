<template>
  <div class="container-fluid">
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchMethod" :fields="fields">
          <template v-slot:cell(uri)="{data}">
            <opensilex-UriLink
              :uri="data.item.uri"
              :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.uri)}"
              :value="data.item.uri"
            ></opensilex-UriLink>
          </template>

          <template v-slot:cell(name)="{data}">{{data.item.name}}</template>

          <template v-slot:cell(species)="{data}">
            <span :key="index" v-for="(uri, index) in data.item.species">
              <span :title="uri">
                {{
                  speciesByUri.get(uri)
                  ? speciesByUri.get(uri).name
                  : undefined
                }}
              </span>
              <span v-if="index + 1 < data.item.species.length">,</span>
            </span>
          </template>

          <template v-slot:cell(startDate)="{data}">
            <opensilex-DateView :value="data.item.start_date"></opensilex-DateView>
          </template>
          <template v-slot:cell(endDate)="{data}">
            <opensilex-DateView :value="data.item.end_date"></opensilex-DateView>
          </template>

          <template v-slot:cell(state)="{data}">
            <i
              v-if="!isEnded(data.item)"
              class="ik ik-activity badge-icon badge-info-opensilex"
              :title="$t('component.experiment.common.status.in-progress')"
            ></i>
            <i
              v-else
              class="ik ik-archive badge-icon badge-light"
              :title="$t('component.experiment.common.status.finished')"
            ></i>
            <i
              v-if="data.item.isPublic"
              class="ik ik-users badge-icon badge-info"
              :title="$t('component.experiment.common.status.public')"
            ></i>
          </template>

           
        </opensilex-TableAsyncView>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {ExperimentGetDTO, SpeciesDTO, SpeciesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

export class ExperimentState {
  code: String;
  label: String;

  constructor(code: String, label: String) {
    this.code = code;
    this.label = label;
  }
}

@Component
export default class ExperimentList extends Vue {
  $opensilex: any;
  $store : any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  speciesByUri: Map<String, SpeciesDTO> = new Map<String, SpeciesDTO>();

  @Prop()
  searchMethod : () => Promise<HttpResponse<OpenSilexResponse<Array<ExperimentGetDTO>>>>;

  created() {
  let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );

    this.$opensilex.disableLoader();
    service
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
          this.speciesByUri.set(
            http.response.result[i].uri,
            http.response.result[i]
          );
        }
        this.$opensilex.enableLoader();
      })
      .catch(this.$opensilex.errorHandler);
  }

  experimentStates: Array<ExperimentState> = [
    {
      code: "in-progress",
      label: "component.experiment.common.status.in-progress"
    },
    {
      code: "finished",
      label: "component.experiment.common.status.finished"
    },
    {
      code: "public",
      label: "component.experiment.common.status.public"
    }
  ];

  isEnded(experiment: ExperimentGetDTO) {
    if (experiment.end_date) {
      return new Date(experiment.end_date).getTime() < new Date().getTime();
    }

    return false;
  }

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
      key: "species",
      label: "component.experiment.species"
    },
    {
      key: "startDate",
      label: "component.experiment.startDate",
      sortable: true
    },
    {
      key: "endDate",
      label: "component.experiment.endDate",
      sortable: true
    },
    {
      key: "state",
      label: "component.experiment.search.column.state"
    },
    {
      label: "component.common.actions",
      key: "actions"
    }
  ];

  
}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentList:
    filter-label: Search by name
    label-filter-placeholder: Enter a name
    filter-year: Search by year
    year-filter-placeholder: Enter a year
    filter-species: Search by species

fr:
  ExperimentList:
    filter-label: Filtrer par nom
    label-filter-placeholder: Saisir un nom
    filter-year: Filtrer par année
    year-filter-placeholder: Saisir une année
    filter-species: Filtrer par espèces
</i18n>