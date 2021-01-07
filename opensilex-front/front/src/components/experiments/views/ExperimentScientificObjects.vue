<template>
  <div>
    <div class="row">
      <div class="col-md-12">
        <opensilex-SearchFilterField @clear="resetSearch()" @search="unselectRefresh()">
          <template v-slot:filters>
            <opensilex-FilterField>
              <b-form-group>
                <label for="name">
                  {{
                  $t("component.common.name")
                  }}
                </label>
                <opensilex-StringFilter
                  id="name"
                  :filter.sync="filters.name"
                  placeholder="ExperimentScientificObjects.name-placeholder"
                ></opensilex-StringFilter>
              </b-form-group>
            </opensilex-FilterField>

            <opensilex-FilterField>
              <b-form-group>
                <label for="type">
                  {{
                  $t("ExperimentScientificObjects.objectType")
                  }}
                </label>
                <opensilex-ScientificObjectTypeSelector
                  id="type"
                  :types.sync="filters.types"
                  :multiple="true"
                  :contextURI="uri"
                ></opensilex-ScientificObjectTypeSelector>
              </b-form-group>
            </opensilex-FilterField>

            <opensilex-FilterField>
              <b-form-group>
                <label for="parentFilter">
                  {{
                  $t("ExperimentScientificObjects.parent-label")
                  }}
                </label>
                <opensilex-SelectForm
                  id="parentFilter"
                  :selected.sync="filters.parent"
                  :multiple="false"
                  :required="false"
                  :searchMethod="searchParents"
                ></opensilex-SelectForm>
              </b-form-group>
            </opensilex-FilterField>

            <opensilex-FilterField>
              <b-form-group>
                <label for="factorLevels">
                  {{
                  $t("FactorLevelSelector.label")
                  }}
                </label>
                <opensilex-FactorLevelSelector
                  id="factorLevels"
                  :factorLevels.sync="filters.factorLevels"
                  :multiple="true"
                  :required="false"
                  :experimentURI="uri"
                ></opensilex-FactorLevelSelector>
              </b-form-group>
            </opensilex-FilterField>
          </template>
        </opensilex-SearchFilterField>
      </div>
    </div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <div class="button-zone">
            <opensilex-CreateButton
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                )
              "
              @click="createScientificObject()"
              label="ExperimentScientificObjects.create-scientific-object"
            ></opensilex-CreateButton>&nbsp;
            <!-- <opensilex-ExperimentFacilitySelector :uri="uri" @facilitiesUpdated="refresh"></opensilex-ExperimentFacilitySelector>&nbsp; -->
            <opensilex-OntologyCsvImporter
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                )
              "
              :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
              :validateCSV="validateCSV"
              :uploadCSV="uploadCSV"
              :customColumns="customColumns"
              @csvImported="refresh"
            ></opensilex-OntologyCsvImporter>
            <!-- &nbsp;
            <opensilex-CreateButton
              @click="exportCSV()"
              label="ExperimentScientificObjects.export-csv"
            ></opensilex-CreateButton>-->
          </div>
          <opensilex-TreeViewAsync
            ref="soTree"
            :searchMethod="searchMethod"
            @select="displayScientificObjectDetailsIfNew($event.data.uri)"
          >
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.type)" />
              </span>&nbsp;
              <span>{{ node.title }}</span>
            </template>

            <template v-slot:buttons="{ node }">
              <opensilex-EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="editScientificObject(node)"
                label="ExperimentScientificObjects.edit-scientific-object"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-AddChildButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="createScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.add-scientific-object-child"
                :small="true"
              ></opensilex-AddChildButton>
              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="deleteScientificObject(node)"
                label="ExperimentScientificObjects.delete-scientific-object"
                :small="true"
              ></opensilex-DeleteButton>
            </template>
          </opensilex-TreeViewAsync>
          <opensilex-ModalForm
            v-if="
              user.hasCredential(
                credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
              )
            "
            ref="soForm"
            component="opensilex-OntologyObjectForm"
            createTitle="ExperimentScientificObjects.add"
            editTitle="ExperimentScientificObjects.update"
            modalSize="lg"
            :initForm="initForm"
            :createAction="callScientificObjectCreation"
            :updateAction="callScientificObjectUpdate"
            @onCreate="refresh()"
            @onUpdate="refresh()"
          >
            <template v-slot:customFields="{ form }">
              <opensilex-SelectForm
                label="ExperimentScientificObjects.parent-label"
                :selected.sync="form.parent"
                :multiple="false"
                :required="false"
                :searchMethod="searchParents"
                :itemLoadingMethod="getParentsByURI"
              ></opensilex-SelectForm>

              <opensilex-GeometryForm
                :value.sync="form.geometry"
                label="component.common.geometry"
                helpMessage="component.common.geometry-help"
              ></opensilex-GeometryForm>
            </template>
          </opensilex-ModalForm>
        </b-card>
      </div>
      <div class="col-md-6">
        <opensilex-ScientificObjectDetail v-if="selected" :selected="selected" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ScientificObjectsService } from "opensilex-core/index";
import HttpResponse from "opensilex-core/HttpResponse";
@Component
export default class ExperimentScientificObjects extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $t: any;
  soService: ScientificObjectsService;
  uri: string;

  @Ref("soForm") readonly soForm!: any;
  @Ref("soTree") readonly soTree!: any;
  @Ref("csvImporter") readonly csvImporter!: any;

  get customColumns() {
    return [
      {
        id: "geometry",
        label: this.$t("ExperimentScientificObjects.geometry-label"),
        type: "WKT",
        comment: this.$t("ExperimentScientificObjects.geometry-comment"),
        isRequired: false,
        isList: false
      }
    ];
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  public nodes = [];

  public filters = {
    name: "",
    types: [],
    parent: undefined,
    factorLevels: []
  };

  public selected = null;

  @Ref("facilitySelector") readonly facilitySelector!: any;

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);

    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.refresh();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.refresh();
        if (this.selected) {
          this.displayScientificObjectDetails(this.selected.uri);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  resetSearch() {
    this.resetFilters();
    this.refresh();
  }

  unselectRefresh() {
    this.selected = null;
    this.refresh();
  }

  resetFilters() {
    this.filters = {
      name: "",
      types: [],
      parent: undefined,
      factorLevels: []
    };
    // Only if search and reset button are use in list
  }

  refresh() {
    if (this.soTree) {
      this.soTree.refresh();
    }
  }

  loadAllChildren(node) {
    let nodeURI = node.data.uri;

    let root = this.nodes[node.path[0]];
    for (let i = 1; i < node.path.length; i++) {
      root = root.children[node.path[i]];
    }

    this.soService
      .getScientificObjectsChildren(this.uri, nodeURI)
      .then(http => {
        let childrenNodes = [];
        for (let i in http.response.result) {
          let soDTO = http.response.result[i];

          let soNode = {
            title: soDTO.name,
            data: soDTO,
            isLeaf: [],
            children: [],
            isExpanded: true,
            isSelected: false,
            isDraggable: false,
            isSelectable: true
          };
          childrenNodes.push(soNode);
        }

        root.children = childrenNodes;
      });
  }

  searchMethod(nodeURI, page, pageSize) {
    if (
      this.filters.name == "" &&
      this.filters.types.length == 0 &&
      this.filters.parent == undefined &&
      this.filters.factorLevels.length == 0
    ) {
      return this.soService.getScientificObjectsChildren(
        this.uri,
        nodeURI,
        undefined,
        page,
        pageSize
      );
    } else {
      return this.soService.searchScientificObjects(
        this.uri,
        this.filters.name,
        this.filters.types,
        this.filters.parent,
        undefined,
        undefined,
        this.filters.factorLevels,
        undefined,
        page,
        pageSize
      );
    }
  }

  searchParents(query, page, pageSize) {
    return this.soService
      .searchScientificObjects(
        this.uri,
        query,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        page,
        pageSize
      )
      .then(http => {
        let nodeList = [];
        for (let so of http.response.result) {
          nodeList.push({
            id: so.uri,
            label: so.name + " (" + so.typeLabel + ")"
          });
        }
        http.response.result = nodeList;
        return http;
      });
  }

  getParentsByURI(soURIs) {
    return this.soService
      .getScientificObjectsListByUris(this.uri, soURIs)
      .then(http => {
        let nodeList = [];
        for (let so of http.response.result) {
          nodeList.push({
            id: so.uri,
            label: so.name + " (" + so.typeLabel + ")"
          });
        }
        return nodeList;
      });
  }

  editScientificObject(node) {
    this.soForm
      .getFormRef()
      .setContext({
        experimentURI: this.uri
      })
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.soService
      .getScientificObjectDetail(this.uri, node.data.uri)
      .then(http => {
        let form: any = http.response.result;

        this.parentURI = form.parent;
        this.soForm.showEditForm(form);
      });
  }

  parentURI;
  createScientificObject(parentURI?) {
    this.soForm
      .getFormRef()
      .setContext({
        experimentURI: this.uri
      })
      .setBaseType(this.$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI);

    this.parentURI = parentURI;
    this.soForm.showCreateForm();
  }

  initForm(form) {
    if (this.parentURI) {
      form.parent = this.parentURI;
    }

    return form;
  }

  public displayScientificObjectDetailsIfNew(nodeUri: any) {
    if (!this.selected || this.selected.uri != nodeUri) {
      this.displayScientificObjectDetails(nodeUri);
    }
  }

  public displayScientificObjectDetails(nodeUri: any) {
    this.soService.getScientificObjectDetail(this.uri, nodeUri).then(http => {
      this.selected = http.response.result;
    });
  }

  public deleteScientificObject(node: any) {
    this.soService
      .deleteScientificObject(this.uri, node.data.uri)
      .then(http => {
        if (this.selected.uri == http.response.result) {
          this.selected = null;
          this.soTree.refresh();
        }
      });
  }

  callScientificObjectCreation(form) {
    let definedRelations = [];
    for (let i in form.relations) {
      let relation = form.relations[i];
      if (relation.property == "vocabulary:isPartOf") {
        relation.value = form.parent;
      }
      if (relation.value != null) {
        if (Array.isArray(relation.value)) {
          for (let j in relation.value) {
            definedRelations.push({
              property: relation.property,
              value: relation.value[j]
            });
          }
        } else {
          definedRelations.push(relation);
        }
      }
    }

    return this.soService
      .createScientificObject({
        uri: form.uri,
        name: form.name,
        type: form.type,
        geometry: form.geometry,
        context: this.uri,
        relations: definedRelations
      })
      .then(http => {
        this.refresh();
      });
  }

  callScientificObjectUpdate(form) {
    let definedRelations = [];
    let parentSet = false;
    for (let i in form.relations) {
      let relation = form.relations[i];
      if (relation.property == "vocabulary:isPartOf") {
        relation.value = form.parent;
        parentSet = true;
      }
      if (relation.value != null) {
        if (Array.isArray(relation.value)) {
          for (let j in relation.value) {
            definedRelations.push({
              property: relation.property,
              value: relation.value[j]
            });
          }
        } else {
          definedRelations.push(relation);
        }
      }
    }

    if (!parentSet && form.parent != null) {
      definedRelations.push({
        property: "vocabulary:isPartOf",
        value: form.parent
      });
    }

    return this.soService
      .updateScientificObject({
        uri: form.uri,
        name: form.name,
        type: form.type,
        geometry: form.geometry,
        context: this.uri,
        relations: definedRelations
      })
      .then(http => {
        this.refresh();
        this.displayScientificObjectDetails(form.uri);
      });
  }

  validateCSV(objectType, csvFile) {
    return this.$opensilex.uploadFileToService(
      "/core/scientific-object/csv-validate",
      {
        description: {
          context: this.uri,
          type: objectType
        },
        file: csvFile
      }
    );
  }

  uploadCSV(objectType, validationToken, csvFile) {
    return this.$opensilex.uploadFileToService(
      "/core/scientific-object/csv-import",
      {
        description: {
          context: this.uri,
          type: objectType,
          validationToken: validationToken
        },
        file: csvFile
      }
    );
  }

  exportCSV(objectType) {}
}
</script>

<style scoped lang="scss">
.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}
</style>

<i18n>
en:
  ExperimentScientificObjects:
    import-scientific-objects: Import experiment objets
    add: Add experiment object
    update: Update experiment object
    create-scientific-object: Create scientific object
    edit-scientific-object: Edit scientific object
    delete-scientific-object: Delete scientific object
    add-scientific-object-child: Add scientific object child
    parent-label: Parent
    load-more: Load more...
    export-csv: Export CSV
    geometry-label: Geometry
    geometry-comment: Geospacial coordinates
    objectType: Object type
    name-placeholder: Enter a name

fr:
  ExperimentScientificObjects:
    import-scientific-objects:  Importer des objets scientifiques
    add: Ajouter un objet scientifique
    update: Mettre à jour un objet scientifiques
    create-scientific-object: Créer un objet scientifique
    edit-scientific-object:  Mettre à jour l'objet scientifique
    delete-scientific-object: Supprimer l'objet scientifique
    add-scientific-object-child: Ajouter un objet scientifique enfant
    parent-label: Parent
    load-more: Charger plus...
    export-csv: Exporter en CSV
    geometry-label: Géometrie
    geometry-comment: Coordonnées géospatialisées
    objectType: Type d'objet
    name-placeholder: Saisir un nom
</i18n>
