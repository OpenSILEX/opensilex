<template>
  <ValidationObserver ref="entityValidatorRef">

    <div>
      <ValidationObserver ref="validatorRef">
        <b-form>
          <b-row>
            <b-col md="6">
              <b-form-group
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>
                  <b-row align-h="left">
                    {{$t('AgroportalExternalReferencesFormPart.search-mapping-title')}}
                  </b-row>
                </template>

              </b-form-group>

              <div v-if="includeAgroportalSearch && isAgroportalReachable">
                <opensilex-AgroportalSearch
                    label="component.common.name"
                    type="text"
                    :placeholder="props.searchPlaceholder"
                    :selected.sync="ontologies"
                    :isAllOntologies.sync="isAllOntologies"
                    @change="onSearchTextChange"
                ></opensilex-AgroportalSearch>

                <opensilex-AgroportalResults
                    ref="searchResultsRef"
                    :text.sync="text"
                    :ontologies.sync="ontologies"
                    :isMappingMode="true"
                    :mappingOptions="skosRelationOptions"
                    @importMapping="onImportMapping">
                </opensilex-AgroportalResults>
              </div>

              <b-form-group
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>
                  <b-row
                      align-h="left"
                      id="manual-mapping"
                  >
                    {{$t('AgroportalExternalReferencesFormPart.map-manually-title')}}
                  </b-row>
                </template>

              </b-form-group>

              <b-row>
                <!-- URI -->
                <opensilex-FilterField
                    :fullWidth="true"
                >
                  <b-form-group>
                    <div class="helperAndBlueStar"> <!-- petite triche pour faire apparaitre l'étoile en bleu -->
                      <opensilex-FormInputLabelHelper
                          label="AgroportalExternalReferencesFormPart.manual-mapping"
                          helpMessage="AgroportalExternalReferencesFormPart.ontologies-help"
                      ></opensilex-FormInputLabelHelper>
                    </div>
                    <ValidationProvider
                        :name="$t('component.skos.uri')"
                        :rules="{
                          required: true,
                          //@todo pourquoi cette regex qui ne correspond pas à la définition d'URI ?
                          regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/
                        }"
                        v-slot="{ errors }"
                    >
                    <span
                        class="error-message alert alert-danger"
                        v-if="isIncludedInRelations()"
                    >{{$t('component.skos.external-already-existing')}}</span>

                      <b-input-group>
                      <b-input
                          id="externalUri"
                          v-model.trim="currentExternalUri"
                          type="text"
                          required
                          :placeholder="$t('component.skos.uri-placeholder')"
                          debounce="300"
                      >
                      </b-input>
                        <template #append>
                          <b-dropdown
                              dropdown
                              :small="true"
                              text="Map term as">

                            <b-dropdown-item v-for="(relation, index) in skosRelationOptions" v-bind:key="relation.id"
                                             class="btn-dropdown"
                                             @click="addRelationToTerm(relation.id)"
                            >
                              {{ relation.label }}
                            </b-dropdown-item>
                          </b-dropdown>
                        </template>
                      </b-input-group>

                      <div class="error-message alert alert-danger">{{ errors[0] }}</div>

                    </ValidationProvider>
                  </b-form-group>

                </opensilex-FilterField>
              </b-row>
            </b-col>

            <b-col md="6">
              <b-form-group
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>
                  <b-row align-h="left">
                    {{$t("AgroportalSearchFormPart.selected-term")}}
                  </b-row>
                </template>

              </b-form-group>

              <b-container class="result">
                <b-row class="mx-0 jqx-max-size">
                  <b-col col lg="12">
                    <div class="result-name">
                      {{ formDto.name }}
                    </div>
                    <div>
                      <a v-bind:href="formDto.uri" target="_blank" rel="noopener noreferrer">{{ formDto.uri }}</a>
                    </div>
                  </b-col>
                </b-row>

                <b-row class="mx-0 jqx-max-size">
                  <b-col col lg="12">
                    {{ formDto.description }}
                  </b-col>
                </b-row>
              </b-container>

              <b-table v-if="relations.length !== 0"
                       striped
                       hover
                       small
                       responsive
                       sort-icon-left
                       bordered
                       :items="relations"
                       :fields="fields">
                <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
                <template v-slot:cell(relation)="data">
                  <b-dropdown
                      dropdown
                      boundary="window"
                      :small="true"
                      :text="$t(data.value)">
                      <b-dropdown-item v-for="relation in skosRelationOptions" v-bind:key="relation.id"
                                       class="btn-dropdown"
                                       @click="updateRelation(relation.id, data.item.relationURI);"
                      >
                        {{ $t(relation.label) }}
                        <font-awesome-icon
                            icon="question-circle"
                            v-b-tooltip.hover.top.html="$t(relation.title)"
                        />
                    </b-dropdown-item>
                  </b-dropdown>
                </template>
                <template v-slot:head(relationURI)="data">{{$t(data.label)}}</template>
                <template v-slot:cell(relationURI)="data">
                  <a :href="data.value" target="_blank">{{ data.value }}</a>
                </template>
                <template v-slot:head(actions)="data">{{$t(data.label)}}</template>
                <template v-slot:cell(actions)="data">
                  <div class="text-center">
                    <b-button-group size="md">
                      <b-button
                          size="md"
                          @click="removeRelationsToSkosReferences(data.item.relationURI)"
                          variant="danger"
                      >
                        <opensilex-Icon icon="fa#trash-alt" />
                      </b-button>
                    </b-button-group>
                  </div>
                </template>
              </b-table>
              <p v-else>
                <strong>{{$t('component.skos.no-external-links-provided')}}</strong>
              </p>
            </b-col>
          </b-row>
        </b-form>
      </ValidationObserver>

    </div>

  </ValidationObserver>
</template>

<script lang="ts">

import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import SUPPORTED_SKOS_RELATIONS from "../../../../models/SkosRelations";
import AgroportalResults from "./AgroportalResults.vue";
import {SelectableItem} from "../../../common/forms/SelectForm.vue";


@Component
export default class AgroportalExternalReferencesFormPart extends Vue {

  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $t: any;
  $i18n: any;

  agroportalAPIService: AgroportalAPIService;

  @PropSync("form")
  formDto: any;

  @Prop()
  props;

  currentRelation: string = "";
  currentExternalUri: string = "";
  text: string = "";
  ontologies: string[] = [];
  isAllOntologies: boolean = false;

  @Ref("validatorRef") readonly validatorRef!: any;
  @Ref("searchResultsRef") readonly searchResultsRef!: AgroportalResults;

  @Prop({default: true})
  displayInsertButton: boolean;

  includeAgroportalSearch: boolean = true;
  isAgroportalReachable: boolean = false;

  checkAgroportalReachable() {
    this.agroportalAPIService.pingAgroportal(1000).then((http) => {
      if (http && http.response) {
        let isReachable = http.response.result;
        this.isAgroportalReachable = isReachable;
      }
    });
  }

  relationsInternal: any[] = [];

  skosRelationOptions: Array<SelectableItem> = [];

  setOptions() {
    this.skosRelationOptions = [];
    for (let skosRelation of SUPPORTED_SKOS_RELATIONS) {
      this.$set(this.skosRelationOptions, this.skosRelationOptions.length, {
        id: skosRelation.dtoKey,
        label: this.$t(skosRelation.label),
        title: this.$t(skosRelation.description)
      });
    }
  }

  created() {
    this.setOptions();
    this.agroportalAPIService = this.$opensilex.getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
    this.checkAgroportalReachable();
    this.ontologies = this.$opensilex.getConfig().agroportal[this.props.ontologiesConfig];
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        () => {this.setOptions();}
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  resetForm() {
    this.currentRelation = "";
    this.currentExternalUri = "";
  }

  resetExternalUriForm() {
    this.currentExternalUri = "";
    this.$nextTick(() => this.validatorRef.reset());
  }

  fields = [
    {
      key: "relation",
      label: "component.skos.relation",
      sortable: true
    },
    {
      key: "relationURI",
      label: "component.skos.uri",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  get relations() {
    this.relationsInternal = [];
    if (this.formDto !== undefined) {
      for (let skosRelation of SUPPORTED_SKOS_RELATIONS) {
        this.updateRelations(skosRelation.dtoKey, this.formDto[skosRelation.dtoKey]);
      }
    }
    return this.relationsInternal;
  }

  updateRelation(relation: string, relationURI: string) {
    this.removeRelationsToSkosReferences(relationURI);
    this.addRelationWithURI(relation, relationURI);
  }

  updateRelations(relation: string, references: string[]) {
    if(references !== undefined){
      for (let index = 0; index < references.length; index++) {
        const element = references[index];
        this.addRelation(relation, element);
      }
    }
  }

  addRelation(relation: string, externalUri: string) {
    this.$set(this.relationsInternal, this.relationsInternal.length, {
      relation: [...SUPPORTED_SKOS_RELATIONS].find(r => r.dtoKey === relation).label,
      relationURI: externalUri
    });
  }

  validateForm() {
    let validatorRef: any = this.$refs.validatorRef;
    return validatorRef.validate();
  }

  validateURIFormat(uri: string): boolean {
    let regex: RegExp;
    regex = /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([-.][a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/;
    return (regex.exec(uri) != null);
  }

  addRelationWithURI(relation: string, relationURI: string) {
    this.currentExternalUri = relationURI;
    this.currentRelation = relation;
    this.addRelationToTerm(relation);
  }

  addRelationToTerm(relation: string) {
    let isIncludedInRelations = this.isIncludedInRelations();
    let isValidUri = this.validateURIFormat(this.currentExternalUri);
    if (!isIncludedInRelations && isValidUri) {
      this.formDto[relation].push(this.currentExternalUri);
      this.resetExternalUriForm();
    }
  }

  isIncludedInRelations(): boolean {
    if (
        this.currentExternalUri == undefined ||
        this.currentExternalUri == "" ||
        this.currentExternalUri.length == 0
    ) {
      return false;
    }
    let includedInRelations = false;
    for (let skosRelation of SUPPORTED_SKOS_RELATIONS) {
      if (this.formDto[skosRelation.dtoKey].includes(this.currentExternalUri)) {
        includedInRelations = true;
        break;
      }
    }
    return includedInRelations;
  }

  removeRelationsToSkosReferences(relationURI: string) {
    for (let skosRelation of SUPPORTED_SKOS_RELATIONS) {
      this.formDto[skosRelation.dtoKey] = this.formDto[skosRelation.dtoKey].filter(function (
          value,
          index,
          arr
      ) {
        return value != relationURI;
      });
    }
    return new Promise((resolve, reject) => {
      this.$emit("onDelete", this.formDto, result => {
        if (result instanceof Promise) {
          result.then(resolve).catch(reject);
        } else {
          resolve(result);
        }
      });
    });
  }

  async update() {
    return new Promise((resolve, reject) => {
      this.$emit("onUpdate", this.formDto, result => {
        if (result instanceof Promise) {
          result.then(resolve).catch(reject);
        } else {
          resolve(result);
        }
      });
    });
  }

  onSearchTextChange(searchedText: string) {
    this.text = searchedText;
    this.searchResultsRef.updateResults(searchedText, this.isAllOntologies);
  }

  onImportMapping(entity: EntityAgroportalDTO, relation) {
    this.currentExternalUri = entity.id;
    this.currentRelation = relation.id;
    this.addRelationToTerm(relation.id);
  }
}

</script>

<style scoped>

a {
  color: #007bff;
}

.helperAndBlueStar {
  display: flex;
}

.result-name {
  font-weight: bold;
  font-size: large;
  margin-bottom: 5px;
}

#manual-mapping {
  padding-top: 10px;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 10px;
  margin-right: 1px;
}

ul {
  list-style-type: none;
}

</style>

<i18n>
en:
  AgroportalExternalReferencesFormPart:
    uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new entity with an auto-generated URI"
    ontologies-help: "You can find URIs in this locations:
      <li>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">AgroPortal</a></ul>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">BioPortal</a></ul>
      </li>"
    search-mapping-title: Search for mapping...
    map-manually-title: ...Or map manually
    manual-mapping: "URI"
fr:
  AgroportalExternalReferencesFormPart:
    uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
    ontologies-help: "Vous pouvez chercher des URIs via ces portails:
      <li>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">AgroPortal</a></ul>
        <ul style=\"list-style-type: none;\"><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">BioPortal</a></ul>
      </li>"
    search-mapping-title: Rechercher des mapping...
    map-manually-title: ...Ou mapper manuellement
    manual-mapping: "URI"
</i18n>