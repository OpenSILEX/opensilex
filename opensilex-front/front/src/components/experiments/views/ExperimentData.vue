<template>
  <div>
    <opensilex-PageActions>
      <opensilex-CreateButton
        v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
        @click="dataForm.showCreateForm()"
        label="OntologyCsvImporter.import"
        class="greenThemeColor createButton"
      ></opensilex-CreateButton>
      
      <b-button
        @click="exportModal.show()"
        class="exportButton greenThemeColor createButton"
      >
        export
      </b-button>
    </opensilex-PageActions>

    <opensilex-DataExportModal
      ref="exportModal"
      :filter="filter"
    ></opensilex-DataExportModal>

    <template>
      <opensilex-PageContent class="pagecontent">
        <!-- Toggle Sidebar--> 
        <div class="searchMenuContainer"
          v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
          :title="searchFiltersPannel()">
          <div class="searchMenuIcon">
            <i class="icon ik ik-search"></i>
          </div>
        </div>

        <!-- FILTERS -->
        <Transition>
          <div v-show="SearchFiltersToggle">

            <opensilex-SearchFilterField
              ref="searchField"
              :withButton="true"
              label="DataView.filter.label"
              @search="refresh()"
              @clear="clear()"
              :showAdvancedSearch="true"
              class="searchFilterField"
            >
              <template v-slot:filters>
                <!-- Germplasm Group -->
                <div>
                  <opensilex-FilterField>
                    <opensilex-GermplasmGroupSelector
                        label="GermplasmList.filter.germplasm-group"
                        :multiple="false"
                        :germplasmGroup.sync="filter.germplasm_group"
                        class="searchFilter"
                        @handlingEnterKey="refresh()"
                    ></opensilex-GermplasmGroupSelector>
                  </opensilex-FilterField>
                </div>

                <!-- targets -->
                <div>
                  <opensilex-FilterField halfWidth="true">
                    <opensilex-TagInputForm
                      class="overflow-auto searchFilter"                  
                      :value.sync="filter.targets"
                      label="DataView.filter.targets"
                      helpMessage="DataView.filter.targets-help"
                      type="text"
                    ></opensilex-TagInputForm>
                  </opensilex-FilterField>
                </div>

                <!-- Scientific objects -->
                <div>
                  <opensilex-FilterField halfWidth="true">
                    <opensilex-SelectForm
                      ref="soSelector"
                      label="DataView.filter.scientificObjects"
                      placeholder="DataView.filter.scientificObjects-placeholder"
                      :selected.sync="filter.scientificObjects"
                      modalComponent="opensilex-ScientificObjectModalListByExp"
                      :filter.sync="soFilter"
                      :isModalSearch="true"
                      :clearable="true"
                      :multiple="true"
                      @clear="refreshSoSelector"
                      @onValidate="refreshProvComponent"
                      @onClose="refreshProvComponent"
                      :limit="1"
                      class="searchFilter"
                    ></opensilex-SelectForm>
                  </opensilex-FilterField>
                </div>

                <!-- Variables -->
                <div>
                  <opensilex-FilterField halfWidth="true">
                    <opensilex-VariableSelectorWithFilter
                      placeholder="VariableSelector.placeholder-multiple"
                      :variables.sync="filter.variables"
                      :experiment="[uri]"
                      :withAssociatedData="true"
                      class="searchFilter"
                    ></opensilex-VariableSelectorWithFilter>
                  </opensilex-FilterField>
                </div>

                <!-- Provenance -->
                <div>
                  <opensilex-FilterField halfWidth="true" >
                    <opensilex-DataProvenanceSelector
                      ref="provSelector"
                      :provenances.sync="filter.provenance"
                      label="ExperimentData.provenance"
                      @select="loadProvenance"
                      :experiments="[uri]"
                      :targets="filter.scientificObjects"
                      :multiple="false"
                      :viewHandler="showProvenanceDetails"
                      :viewHandlerDetailsVisible="visibleDetails"
                      :key="refreshKey"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                    ></opensilex-DataProvenanceSelector>

                    <b-collapse
                      v-if="selectedProvenance"
                      id="collapse-4"
                      v-model="visibleDetails"
                      class="mt-2"
                    >
                      <opensilex-ProvenanceDetails
                        :provenance="getSelectedProv"
                      ></opensilex-ProvenanceDetails>
                    </b-collapse>
                  </opensilex-FilterField>
                </div>
              </template>

              <template v-slot:advancedSearch>
                <!-- Start Date -->
                <div>  
                  <opensilex-FilterField>
                    <opensilex-DateTimeForm
                      :value.sync="filter.start_date"
                      label="component.common.begin"
                      name="startDate"
                      :max-date="filter.end_date ? filter.end_date : undefined"
                      class="searchFilter"
                    ></opensilex-DateTimeForm>
                  </opensilex-FilterField>
                </div>

                <!-- End Date -->
                <div>
                  <opensilex-FilterField>
                    <opensilex-DateTimeForm
                      :value.sync="filter.end_date"
                      label="component.common.end"
                      name="endDate"
                      :min-date="filter.start_date ? filter.start_date : undefined"
                      class="searchFilter"
                    ></opensilex-DateTimeForm>
                  </opensilex-FilterField>
                </div>
              </template>
            </opensilex-SearchFilterField>
          </div>
        </Transition>
        <div class="card">
          <div class="card-body">
            <opensilex-DataList
              ref="dataList"
              :listFilter.sync="filter"
              :contextUri="uri"
              class="dataList">
            </opensilex-DataList>
          </div>
        </div>
      </opensilex-PageContent>
    </template>

    <opensilex-ModalForm
      ref="dataForm"
      :initForm="initFormData"
      createTitle="DataImportForm.create"
      editTitle="DataImportForm.update"
      component="opensilex-DataImportForm"
      icon="ik#ik-bar-chart-line"
      modalSize="xl"
      @onCreate="afterCreateData"
      :successMessage="successMessage"
    ></opensilex-ModalForm>

    <opensilex-ResultModalView
      ref="resultModal"
      @onHide="refreshDataAfterImportation()"
    >            
    </opensilex-ResultModalView>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ScientificObjectNodeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ExperimentData extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;
  uri = null;
  visibleDetails: boolean = false;
  searchVisible: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  refreshKey = 0;

  filter = {
    germplasm_group: undefined,
    start_date: null,
    end_date: null,
    provenance: null,
    variables: [],
    experiments: [this.uri],
    scientificObjects: [],
    targets: [],
    devices: [],
    facilities: [],
    operators: []
  };

  soFilter = {
    name: "",
    experiment: this.uri,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  };

  data(){
    return {
      SearchFiltersToggle : false,
    }
  }

  @Ref("dataList") readonly dataList!: any;
  @Ref("dataForm") readonly dataForm!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("soSelector") readonly soSelector!: any;
  @Ref("varSelector") readonly varSelector!: any;
  @Ref("exportModal") readonly exportModal!: any;

  get credentials() {
    return this.$store.state.credentials;
  }

  get user() {
    return this.$store.state.user;
  }

  refreshProvComponent() {
    this.refreshKey += 1;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.resetFilters();

    this.soFilter = {
      name: "",
      experiment: this.uri,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };  
  }

  resetFilters() {
    this.filter = {
      germplasm_group: undefined,
      start_date: null,
      end_date: null,
      provenance: null,
      variables: [],
      experiments: [this.uri],
      scientificObjects: [],
      targets: [],
      devices: [],
      facilities: [],
      operators: []
    };
    // Only if search and reset button are use in list
  }

  refreshSoSelector() {
    
    this.soSelector.refreshModalSearch();
    this.refreshProvComponent();
  }

  successMessage(form) {
    return this.$t("ResultModalView.data-imported");
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  refreshDataAfterImportation() {
    this.loadProvenance({ id: this.filter.provenance });
    this.refresh();
  }

  afterCreateData(results) {
    if (results instanceof Promise) {
      results.then((res) => {
        this.resultModal.setNbLinesImported(
          res.validation.dataErrors.nbLinesImported
        );
        this.resultModal.setProvenance(res.form.provenance);
        this.resultModal.show();
        this.clear();
        this.filter.provenance = res.form.provenance.uri;
        this.refreshVariables();
        this.refreshKey += 1;
        this.loadProvenance({ id: res.form.provenance.uri });
      });
    } else {
      this.resultModal.setNbLinesImported(
        results.validation.dataErrors.nbLinesImported
      );
      this.resultModal.setProvenance(results.form.provenance);
      this.resultModal.show();
      this.clear();
      this.filter.provenance = results.form.provenance.uri;
      this.refreshVariables();
      this.refreshKey += 1;
      this.loadProvenance({ id: results.form.provenance.uri });
    }
  }

  initFormData(form) {
    form.experiment = this.uri;
    return form;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  clear() {
    this.searchVisible = false;
    this.selectedProvenance = null;
    this.resetFilters();
    this.refresh();
  }

  mounted() {
    this.searchVisible = false;
    this.refreshVariables();
  }

  refreshVariables() {
    this.$opensilex
      .getService("opensilex.DataService")
      .getUsedVariables([this.uri], null, null, null)
      .then((http) => {
        let variables = http.response.result;
        this.usedVariables = [];
        for (let i in variables) {
          let variable = variables[i];
          this.usedVariables.push({
            id: variable.uri,
            label: variable.name,
          });
        }
      });
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.selectedProvenance = prov;
      });
    }
  }

  refresh() {
    this.searchVisible = true;
    this.dataList.refresh();
    //remove experiments filter from URL
    this.$nextTick(() => {
      this.$opensilex.updateURLParameter("experiments", null, "");
    });
  }

  loadSO(scientificObjectsURIs) {
    const sos = scientificObjectsURIs.filter((x, i, a) => a.indexOf(x) == i); // distinct element on array
    return this.$opensilex
      .getService("opensilex.ScientificObjectsService")
      .getScientificObjectsListByUris(this.uri, sos)
      .then(
        (
          http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>
        ) => {
          return http && http.response ? http.response.result : undefined;
        }
      )
      .catch(this.$opensilex.errorHandler);
  }

  soGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name,
      };
    }
    return null;
  }

  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }
}
</script>

<style scoped lang="scss">

.pagecontent {
 margin-top: 10px
}

.createButton {
  margin-top: 10px;
}
.card-body {
  margin-bottom: -15px;
}
</style>

<i18n>
en:
    ExperimentData:
        object: Scientific Object
        date: Date
        value: Value
        variable : Variable
        provenance: Provenance
        export : Export
        export-wide : Wide format
        export-wide-help : A given date, provenance, scientific object of an observation represents a row and each variable value is in a specific column.
        export-long : Long format
        export-long-help : Each line represent an observation (Same as the result table)
fr:
    ExperimentData:
        object: Objet Scientifique
        date: Date
        value: Valeur
        variable : Variable
        provenance: Provenance
        export : Exporter
        export-wide : Format large
        export-wide-help : Une date, une provenance, un objet scientifique donné d'une observation représente une ligne et chaque valeur de variable est dans une colonne spécifique.
        export-long : Format long
        export-long-help : Une ligne représente une observation (identique au tableau de résultat)

</i18n>
