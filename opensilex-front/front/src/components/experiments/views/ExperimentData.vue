<template>
  <div>
    <div v-if="uri" class="row">
      <div class="col col-xl-12">
        <div class="card">
          <div class="card-header">
            <h3>
              <i class="ik ik-clipboard"></i>
              {{ $t('ExperimentView.data') }}
            </h3>
          </div>
          <div class="card-body">
            <opensilex-SelectForm
              label="Variable"
              :required="true"
              :selected.sync="selectedVariable"
              :options="usedVariables"
              @select="tableRef.refresh()"
            ></opensilex-SelectForm>

            <opensilex-TableAsyncView
              v-if="selectedVariable"
              ref="tableRef"
              :searchMethod="searchData"
              :fields="fields"
            >
              <template v-slot:cell(object)="{data}">
                <opensilex-UriLink :uri="data.item.object" :value="objects[data.item.object]"></opensilex-UriLink>
              </template>

              <template v-slot:cell(date)="{data}">
                <opensilex-DateView :value="data.item.date" />
              </template>

              <template v-slot:cell(value)="{data}">
                <div>{{data.item.value}}</div>
              </template>
            </opensilex-TableAsyncView>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentCreationDTO,
  ExperimentGetDTO,
  ExperimentsService,
  ProjectCreationDTO,
  InfrastructureGetDTO,
  ProjectsService,
  InfrastructuresService,
  SpeciesDTO,
  SpeciesService,
  FactorsService,
  FactorGetDTO
} from "opensilex-core/index";
import {
  SecurityService,
  GroupDTO,
  UserGetDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class ExperimentData extends Vue {
  $opensilex: any;
  $t: any;

  uri = null;

  @Ref("tableRef") readonly tableRef!: any;

  created() {
    this.uri = this.$route.params.uri;
  }

  usedVariables = [];

  selectedVariable = null;

  mounted() {
    this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getUsedVariables(this.uri)
      .then(http => {
        let variables = http.response.result;
        this.usedVariables = [];
        for (let i in variables) {
          let variable = variables[i];
          this.usedVariables.push({
            id: variable.uri,
            label: variable.name
          });
        }
      });
  }

  fields = [
    {
      key: "object",
      label: "ExperimentData.object"
    },
    {
      key: "date",
      label: "ExperimentData.date"
    },
    {
      key: "value",
      label: "ExperimentData.value"
    }
  ];

  objects = {};

  searchData(options) {
    return new Promise((resolve, reject) => {
      this.$opensilex
        .getService("opensilex.DataService")
        .searchDataList(
          undefined, // objectUri
          this.selectedVariable, // variableUri
          undefined, // provenanceUri
          undefined, // startDate
          undefined, // endDate
          options.currentPage,
          options.pageSize
        )
        .then(http => {
          let objectToLoad = [];
          for (let i in http.response.result) {
            let objectURI = http.response.result[i].object;
            if (!this.objects[objectURI] && !objectToLoad.includes(objectURI)) {
              objectToLoad.push(objectURI);
            }
          }

          if (objectToLoad.length > 0) {
            this.$opensilex
              .getService("opensilex.ScientificObjectsService")
              .getScientificObjectsListByUris(this.uri, objectToLoad)
              .then(httpObj => {
                for (let j in httpObj.response.result) {
                  let obj = httpObj.response.result[j];
                  this.objects[obj.uri] = obj.name + " (" + obj.typeLabel + ")";
                }
                resolve(http);
              })
              .catch(reject);
          } else {
            resolve(http);
          }
        })
        .catch(reject);
    });
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ExperimentData:
        object: Object
        date: Date
        value: Value

fr:
    ExperimentData:
        object: Objet
        date: Date
        value: Valeur
</i18n>