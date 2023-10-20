<template>
  <ValidationObserver ref="entityValidatorRef">

    <div>
      <ValidationObserver ref="validatorRef">
        <b-form>
          <p v-if="entityDto.uri">
            {{$t('component.skos.addTo')}}
            <em>
              <strong class="text-primary">{{this.entityDto.uri}}</strong>
            </em>
          </p>

          <b-row>
            <b-col md="6">
              <b-form-group
                  label-size="lg"
                  label-class="font-weight-bold pt-0"
                  class="mb-0"
              >
                <template v-slot:label>
                  <b-row align-h="left">
                    Search for mapping
                  </b-row>
                </template>

              </b-form-group>

              <div v-if="includeAgroportalSearch && isAgroportalReachable">
                <opensilex-AgroportalSearch
                    label="component.common.name"
                    type="text"
                    placeholder="search"
                    :selected.sync="ontologies"
                    :isAllOntologies.sync="isAllOntologies"
                    @change="onSearchTextChange"
                ></opensilex-AgroportalSearch>

                <opensilex-AgroportalResults
                    ref="searchResultsRef"
                    :text.sync="text"
                    :ontologies.sync="ontologies"
                    :isMappingMode="true"
                    :mappingOptions="options"
                    @importMapping="onImportMapping">
                </opensilex-AgroportalResults>
              </div>
              <b-row>
                <!-- URI -->
                <opensilex-FilterField :fullWidth="true">
                  <b-form-group>
                    <div class="helperAndBlueStar"> <!-- petite triche pour faire apparaitre l'étoile en bleu -->
                      <opensilex-FormInputLabelHelper
                          label="component.skos.uri"
                          helpMessage="AgroportalEntityExternalReferencesForm.ontologies-help"
                      ></opensilex-FormInputLabelHelper>
                      <pre class="blueStar"> *</pre>
                    </div>
                    <ValidationProvider
                        :name="$t('component.skos.uri')"
                        :rules="{
                          required: true,
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

                            <b-dropdown-item v-for="(relation, index) in options" v-bind:key="relation.id"
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
                      Selected entity
                  </b-row>
                </template>

              </b-form-group>

              <b-container class="result">
                <b-row class="mx-0 jqx-max-size">
                  <b-col col lg="12">
                    <div class="result-name">
                      {{entityDto.name}}
                    </div>
                    <div>
                      <a v-bind:href="entityDto.uri" target="_blank" rel="noopener noreferrer">{{entityDto.uri}}</a>
                    </div>
                  </b-col>
                </b-row>

                <b-row class="mx-0 jqx-max-size">
                  <b-col col lg="12">
                    {{entityDto.description}}
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
                <template v-slot:cell(relation)="data">{{$t(data.value)}}</template>
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
                          @click="removeRelationsToSkosReferences(data.item)"
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
// @ts-ignore
import {EntityCreationDTO} from "opensilex-core/index";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import AgroportalEntityCreate from "./AgroportalEntityCreate.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {Skos} from "../../../models/Skos";
import AgroportalResults from "../AgroportalResults.vue";
import {LinksAgroportalModel} from "opensilex-core/model/linksAgroportalModel";


@Component
export default class AgroportalEntityExternalReferencesForm extends Vue {

  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $t: any;
  $i18n: any;

  agroportalAPIService: AgroportalAPIService;

  @PropSync("form")
  entityDto: EntityCreationDTO;
  selectedEntity: EntityAgroportalDTO;

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

  ontologiesToSelect: string[] = AgroportalEntityCreate.selectedOntologies;

  checkAgroportalReachable() {
    this.agroportalAPIService.pingAgroportal(1000).then((http) => {
      if (http && http.response) {
        let isReachable = http.response.result;
        this.isAgroportalReachable = isReachable;
      }
    });
  }

  relationsInternal: any[] = [];

  skosRelationsMap: Map<string, string> = Skos.getSkosRelationsMap();

  options: any[] = [];

  entityDtoToAgroportalDto() {
    let dto: EntityAgroportalDTO = new class implements EntityAgroportalDTO {
      definitions: Array<string>;
      id: string;
      links: LinksAgroportalModel;
      name: string;
      obsolete: boolean;
      synonym: Array<string>;
      type: string;
    };
    dto.id = this.entityDto.uri;
    dto.name = this.entityDto.name;
    dto.definitions = [this.entityDto.description];
    dto.links = {};
    dto.synonym = [];
    dto.obsolete = false;
    return dto;
  }

  setOptions(){
    this.options = [];
    for (let [key, value] of this.skosRelationsMap) {
      this.$set(this.options, this.options.length, {
        id: key,
        label: this.$t(value)
      });
    }
  }

  created() {
    this.setOptions();
    this.agroportalAPIService = this.$opensilex.getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
    this.checkAgroportalReachable();
    this.ontologies = this.$opensilex.getConfig().agroportal.entityOntologies;
    this.selectedEntity = this.entityDtoToAgroportalDto();
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
    if (this.entityDto !== undefined) {
      for (let [key, value] of this.skosRelationsMap) {
        this.updateRelations(key, this.entityDto[key]);
      }
    }
    return this.relationsInternal;
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
      relation: this.skosRelationsMap.get(relation),
      relationURI: externalUri
    });
  }

  validateForm() {
    let validatorRef: any = this.$refs.validatorRef;
    return validatorRef.validate();
  }

  addRelationsToSkosReferences() {
    this.validateForm().then(isValid => {
      if (isValid) {
        this.addRelationToSkosReferences();
        return new Promise((resolve, reject) => {
          this.$emit("onAdd", this.entityDto, result => {
            if (result instanceof Promise) {
              result.then(resolve).catch(reject);
            } else {
              resolve(result);
            }
          });
        });
      }
    });
  }

  addRelationToSkosReferences() {
    let isIncludedInRelations = this.isIncludedInRelations();
    if (!isIncludedInRelations) {
      this.entityDto[this.currentRelation].push(this.currentExternalUri);
      this.resetExternalUriForm();
    }
  }

  validateURIFormat(uri: string): boolean {
    let regex: RegExp;
    regex = /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([-.][a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/;
    return (regex.exec(uri) != null);
  }

  addRelationToTerm(relation: string) {
    let isIncludedInRelations = this.isIncludedInRelations();
    let isValidUri = this.validateURIFormat(this.currentExternalUri);
    if (!isIncludedInRelations && isValidUri) {
      this.entityDto[relation].push(this.currentExternalUri);
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
    for (let [key, value] of this.skosRelationsMap) {
      if (this.entityDto[key].includes(this.currentExternalUri)) {
        includedInRelations = true;
        break;
      }
    }
    return includedInRelations;
  }

  removeRelationsToSkosReferences(row: any) {
    for (let [key, value] of this.skosRelationsMap) {
      this.entityDto[key] = this.entityDto[key].filter(function (
          value,
          index,
          arr
      ) {
        return value != row.relationURI;
      });
    }
    return new Promise((resolve, reject) => {
      this.$emit("onDelete", this.entityDto, result => {
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
      this.$emit("onUpdate", this.entityDto, result => {
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
.blueStar {
  color: #007bff;
}

.result-name {
  font-weight: bold;
  font-size: large;
  margin-bottom: 5px;
}

#result-ontology {
  font-weight: normal;
  font-size: medium;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 10px;
  margin-right: 1px;
}

</style>

<i18n>
en:
  AgroportalEntityExternalReferencesForm:
    uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new entity with an auto-generated URI"
    ontologies-help: "You can find URIs in this locations: <li><ul><a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://agroportal.lirmm.fr/\">AgroPortal</a></ul></li>"
fr:
  AgroportalEntityExternalReferencesForm:
    uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
    ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une entité correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de l'entité dans le champ 'Nom'."

</i18n>