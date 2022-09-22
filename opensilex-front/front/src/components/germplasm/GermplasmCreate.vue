<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#seedling"
      title="GermplasmCreate.title"
      description="GermplasmCreate.description"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" >
      <opensilex-HelpButton
      @click="helpModal.show()"
      label="component.common.help-button"
      class="helpButton"
      ></opensilex-HelpButton>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <opensilex-TypeForm
        :type.sync="selectedType"
        :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
        :placeholder="$t('GermplasmCreate.form-type-placeholder')"
        @update:type="refreshTable"
      ></opensilex-TypeForm>

      <opensilex-GermplasmTable v-if="selectedType" ref="germplasmTable" :germplasmType="selectedType" :key="tabulatorRefresh"></opensilex-GermplasmTable>
    </opensilex-PageContent>
    <b-modal ref="helpModal" size="xl" hide-header hide-footer>
      <opensilex-GermplasmHelp @hideBtnIsClicked="hide()"></opensilex-GermplasmHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref } from "vue-property-decorator";
// @ts-ignore
import { OntologyService, ResourceTreeDTO } from "opensilex-core/index";
import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GermplasmCreate extends Vue {
  service: OntologyService;
  $opensilex: any;
  $store: any;
  germplasmTypes: any = [];
  selectedType: string = null;
  tabulatorRefresh = 0;

  @Ref("germplasmTable") readonly germplasmTable!: any;
  @Ref("helpModal") readonly helpModal!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.loadGermplasmTypes();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadGermplasmTypes();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  loadGermplasmTypes() {
    this.germplasmTypes = [];
    let ontoService: OntologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );

    ontoService
      .getSubClassesOf(Oeso.GERMPLASM_TYPE_URI, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        console.log(http.response.result);
        for (let i = 0; i < http.response.result.length; i++) {
          let resourceDTO = http.response.result[i];
          if (Oeso.checkURIs(resourceDTO.uri, Oeso.PLANT_MATERIAL_LOT_TYPE_URI)) {
            //retrieve plantMaterialLot children
            let children = resourceDTO.children;
            for (let j = 0; j < children.length; j++) {
              this.germplasmTypes.push({
                value: children[j].uri,
                text: children[j].name,
              });
            }
          } else {
            this.germplasmTypes.push({
              value: resourceDTO.uri,
              text: resourceDTO.name,
            });
          }
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  refreshTable() {
    let germplasmTable: any = this.germplasmTable;
    console.log(this.selectedType);    
    this.tabulatorRefresh++;
    germplasmTable.updateColumns();
  }
  
  hide() {
    this.helpModal.hide();
  }
}
</script>

<style scoped lang="scss">

.helpButton {
  margin-left: 0px;
  color: #00A28C;
  font-size: 1.2em;
  border: none;
  margin-bottom: 10px;
}
  
.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}
</style>

<i18n>

en:
  GermplasmCreate:
    title: Declare Germplasm
    description: Add species, varieties, accessions ...
    backToList: Return to germplasm list
    form-type-placeholder: Please select a type
fr:
  GermplasmCreate:
    title: Déclarer des ressources génétiques
    description: Créer des espèces, des variétiés, des accessions ...
    backToList: Revenir à la liste des germplasm
    form-type-placeholder: Veuillez sélectionner un type de germplasm
  
</i18n>