<template>
  <div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <b-button-group>
            <opensilex-ExperimentFacilitySelector :uri="uri" @facilitiesUpdated="refresh"></opensilex-ExperimentFacilitySelector>
            <opensilex-ScientificObjectCSVImporter :uri="uri" @csvImported="refresh"></opensilex-ScientificObjectCSVImporter>
          </b-button-group>
          <opensilex-TreeView :nodes.sync="nodes" @select="displayScientificObjectDetails">
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.type)" />
              </span>&nbsp;
              <strong v-if="node.data.selected">{{ node.title }}</strong>
              <span v-if="!node.data.selected">{{ node.title }}</span>
            </template>

            <!-- <template v-slot:buttons="{ node }">
              <opensilex-AddChildButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="addExistingScientificObjectAsChild(node.data.uri)"
                label="ExperimentScientificObjects.add-facility-scientific-objects"
                :small="true"
              ></opensilex-AddChildButton>
              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="deleteScientificObjectRelation(node.data.uri)"
                label="ExperimentScientificObjects.delete-facility-relation"
                :small="true"
              ></opensilex-DeleteButton>
            </template>-->
          </opensilex-TreeView>
        </b-card>
      </div>
      <div class="col-md-6">
        <b-card>SO details</b-card>
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

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  public nodes = [];

  @Ref("facilitySelector") readonly facilitySelector!: any;

  created() {
    this.uri = this.$route.params.uri;

    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.refresh();
  }

  refresh() {
    this.soService.getExperimentScientificObjectsTree(this.uri).then(http => {
      let treeNode = [];
      let first = true;
      for (let i in http.response.result) {
        let resourceTree: PartialResourceTreeDTO = http.response.result[i];
        let node = this.dtoToNode(resourceTree);
        treeNode.push(node);

        this.nodes = treeNode;
      }
    });
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
      isLeaf: isLeaf,
      children: childrenDTOs,
      isExpanded: true,
      isSelected: false,
      isDraggable: false,
      isSelectable: true
    };
  }

  addExistingScientificObjectAsChild(facilityURI) {}

  deleteFacilityRelation(facilityURI) {}

  public displayScientificObjectDetails(node: any) {}
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ExperimentScientificObjects:
    import-scientific-objects: Import experiment objets

fr:
  ExperimentScientificObjects:
    import-scientific-objects:  Importer des objets d'étude
</i18n>
