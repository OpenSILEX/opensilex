<template>
  <div>
    <div class="page-header">
      <div class="row align-items-end">

          <div class="col-lg-2">
              <nav class="breadcrumb-container" aria-label="breadcrumb">
                  <ol class="breadcrumb">
                      <li class="breadcrumb-item">
                        <router-link :to="{path: '/germplasm'}" :title="$t('component.germplasm.backToList')">
                          <i class="ik ik-corner-up-left"></i>
                          {{ $t('component.germplasm.germplasmList') }}
                        </router-link>
                      </li>
                  </ol>
              </nav>
          </div>
      </div>
    </div>
    <div>    
      <b-input-group class="mt-3 mb-3" size="sm">
        <b-form-select ref="germplasmType" 
          v-model="selected" :options="germplasmTypes" 
          @change="refreshTable">
          <template v-slot:first>
            <b-form-select-option :value="null">{{$t('component.germplasm.select')}}</b-form-select-option>
          </template>
        </b-form-select>
      </b-input-group>
      <opensilex-GermplasmTable
      v-if = this.selected
      ref="germplasmTable" 
      :germplasmType=selected
      ></opensilex-GermplasmTable>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from 'vue-property-decorator'
import { GermplasmCreationDTO, OntologyService, ResourceTreeDTO } from "opensilex-core/index"; 
import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GermplasmCreate extends Vue {
  service: OntologyService;
  $opensilex: any;
  germplasmTypes: any = [];
  selected: string = null;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.loadGermplasmTypes();
  }

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {       
        this.loadGermplasmTypes();
      }
    );
  }


  loadGermplasmTypes(){
    this.germplasmTypes = []
    let ontoService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");

    ontoService.getSubClassesOf(Oeso.GERMPLASM_TYPE_URI,true)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      console.log(http.response.result)
        for(let i=0; i<http.response.result.length; i++) {
          let resourceDTO = http.response.result[i];
          if (resourceDTO.uri.endsWith("PlantMaterialLot")) {
            //retrieve plantMaterialLot children
            let children = resourceDTO.children;
            for (let j=0; j<children.length; j++) {
              this.germplasmTypes.push({
                value: children[j].uri,
                text: children[j].name
              });
            }
          } else {
            this.germplasmTypes.push({
                value: resourceDTO.uri,
                text: resourceDTO.name
            });
          }
        }
    }).catch(this.$opensilex.errorHandler);
  
  }
    
  refreshTable() {
    let germplasmTable: any = this.$refs.germplasmTable;
    germplasmTable.updateColumns();
  }  

}

</script>

<style scoped lang="scss">

</style>