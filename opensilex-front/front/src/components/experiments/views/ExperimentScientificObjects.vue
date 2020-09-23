<template>
  <div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <div class="button-zone">
            <opensilex-CreateButton
              v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
              @click="createScientificObject()"
              label="ExperimentScientificObjects.create-scientific-object"
            ></opensilex-CreateButton>&nbsp;
            <!-- <opensilex-ExperimentFacilitySelector :uri="uri" @facilitiesUpdated="refresh"></opensilex-ExperimentFacilitySelector>&nbsp; -->
            <opensilex-OntologyCsvImporter
              :baseType="$opensilex.Oeso.SCIENTIFIC_OBJECT_TYPE_URI"
              :validateCSV="validateCSV"
              :uploadCSV="uploadCSV"
              @csvImported="refresh"
            ></opensilex-OntologyCsvImporter>
          </div>
          <opensilex-TreeView
            :nodes.sync="nodes"
            @select="displayScientificObjectDetailsIfNew($event.data.uri)"
            @toggle="loadChildrenIfRequired($event)"
          >
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.type)" />
              </span>&nbsp;
              <strong v-if="node.data.selected">{{ node.title }}</strong>
              <span v-if="!node.data.selected">{{ node.title }}</span>
              <span
                class="async-tree-action"
                v-if="node.children.length > 0 && node.data.childCount > 0  && node.data.childCount > node.children.length"
              >
                ({{node.data.typeLabel}} - {{node.children.length}}/{{node.data.childCount}}
                <span>
                  -
                  <a
                    href="#"
                    @click.prevent="loadMoreChildren(node)"
                  >{{$t('ExperimentScientificObjects.load-more')}}</a>
                </span>)
              </span>
              <span class="async-tree-action" v-else>&nbsp;({{node.data.typeLabel}})</span>
            </template>

            <template v-slot:buttons="{ node }">
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="editScientificObject(node)"
                label="ExperimentScientificObjects.edit-scientific-object"
                :small="true"
              ></opensilex-EditButton>
              <opensilex-AddChildButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="createScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.add-scientific-object-child"
                :small="true"
              ></opensilex-AddChildButton>
              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="deleteScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.delete-scientific-object"
                :small="true"
              ></opensilex-DeleteButton>
            </template>
          </opensilex-TreeView>
          <opensilex-ModalForm
            v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
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
            <template v-slot:customFields="{form, editMode}">
              <opensilex-SelectForm
                label="ExperimentScientificObjects.parent-label"
                :selected.sync="form.parent"
                :multiple="false"
                :required="false"
                :options="getParentOptions(form, editMode)"
              ></opensilex-SelectForm>
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
import {
  ScientificObjectsService,
  PartialResourceTreeDTO
} from "opensilex-core/index";
import HttpResponse from "opensilex-core/HttpResponse";
@Component
export default class ExperimentScientificObjects extends Vue {
  $opensilex: any;
  soService: ScientificObjectsService;
  uri: string;

  @Ref("soForm") readonly soForm!: any;
  @Ref("csvImporter") readonly csvImporter!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  public nodes = [];

  public selected = null;

  @Ref("facilitySelector") readonly facilitySelector!: any;

  created() {
    this.uri = this.$route.params.uri;

    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.refresh();
  }

  refresh() {
    this.soService.getScientificObjectsTree(this.uri).then(http => {
      let treeNode = [];
      let first = true;
      for (let i in http.response.result) {
        let resourceTree: PartialResourceTreeDTO = http.response.result[i];
        let node = this.dtoToNode(resourceTree);
        treeNode.push(node);
      }
      this.nodes = treeNode;
    });

    this.soService.getScientificObjectsList(this.uri).then(http => {
      this.availableParents = http.response.result;
    });
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

  loadMoreChildren(node) {
    let nodeURI = node.data.uri;

    let root = this.nodes[node.path[0]];
    for (let i = 1; i < node.path.length; i++) {
      root = root.children[node.path[i]];
    }

    if (root.children.length < node.data.childCount) {
      let remainingChildren = node.data.childCount - root.children.length;
      let offset = root.children.length;
      let limit = Math.min(remainingChildren, 40);
      this.soService
        .getScientificObjectsChildren(this.uri, nodeURI, limit, offset)
        .then(http => {
          let childrenNodes = [];
          for (let i in http.response.result) {
            let soDTO = http.response.result[i];

            let soNode = {
              title: soDTO.name,
              data: soDTO,
              isLeaf: soDTO.childCount == 0,
              children: [],
              isExpanded: false,
              isSelected: false,
              isDraggable: false,
              isSelectable: true
            };
            childrenNodes.push(soNode);
          }

          root.children = root.children.concat(childrenNodes);
        });
    }
  }

  private dtoToNode(dto: PartialResourceTreeDTO) {
    let isLeaf = dto.children.length == 0;

    let childrenDTOs = [];
    if (!isLeaf) {
      for (let i in dto.children) {
        childrenDTOs.push(this.dtoToNode(dto.children[i]));
      }
    }

    return {
      title: dto.name,
      data: dto,
      isLeaf: isLeaf && dto.childCount == 0,
      children: childrenDTOs,
      isExpanded: !isLeaf,
      isSelected: false,
      isDraggable: false,
      isSelectable: true
    };
  }

  getParentOptions(form, editMode) {
    let parentOptions = [];
    for (let i in this.availableParents) {
      let availableParent = this.availableParents[i];
      if (!editMode || availableParent.uri != form.uri) {
        parentOptions.push({
          id: availableParent.uri,
          label: availableParent.name + " (" + availableParent.typeLabel + ")"
        });
      } else {
        parentOptions.push({
          id: availableParent.uri,
          label: availableParent.name + " (" + availableParent.typeLabel + ")",
          isDisabled: true
        });
      }
    }
    return parentOptions;
  }

  availableParents = [];

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

  public deleteScientificObject(objectURI: any) {
    this.soService.deleteScientificObject(this.uri, objectURI).then(http => {
      if (this.selected.uri == http.response.result) {
        this.selected = null;
        this.refresh();
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
        experiment: this.uri,
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
        experiment: this.uri,
        relations: definedRelations
      })
      .then(http => {
        this.refresh();
        this.displayScientificObjectDetails(form.uri);
      });
  }

  loadChildrenIfRequired(node) {
    console.error(node);
    if (node.children.length == 0 && node.data.childCount > 0) {
      this.loadMoreChildren(node);
    }
  }

  validateCSV(objectType, csvFile) {
    return this.$opensilex.uploadFileToService(
      "/core/scientific-object/csv-validate",
      {
        description: {
          experiment: this.uri,
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
          experiment: this.uri,
          type: objectType,
          validationToken: validationToken
        },
        file: csvFile
      }
    );
  }
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
</i18n>
