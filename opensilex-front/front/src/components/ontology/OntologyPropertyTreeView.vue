<template>
  <div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <opensilex-TreeView :nodes.sync="nodes" @select="displayPropertyNodeDetail">
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="getPropertyIcon(node)" />
              </span>&nbsp;
              <strong v-if="node.data.selected">{{ node.title }}</strong>
              <span v-if="!node.data.selected">{{ node.title }}</span>
            </template>

            <!-- <template v-slot:buttons="{ node }">
              <opensilex-EditButton
                @click="editOntologyType(node.data.uri)"
                label="OntologyClassTreeView.edit"
                :small="true"
              ></opensilex-EditButton>
            </template>-->
          </opensilex-TreeView>
        </b-card>
      </div>
      <div class="col-md-6">
        <opensilex-OntologyPropertyDetail :selected="selected" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop, Watch } from "vue-property-decorator";
import Vue from "vue";
import { OntologyService, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse from "opensilex-core/HttpResponse";
import OWL from "../../ontologies/OWL";
@Component
export default class OntologyPropertyTreeView extends Vue {
  $opensilex: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  domain;

  public nodes = [];

  public selected = null;

  ontologyService: OntologyService;

  created() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );

    this.onDomainChange();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.onDomainChange();
        if (this.selected) {
          this.displayPropertyDetail(this.selected.uri, this.selected.type);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  @Watch("domain")
  onDomainChange() {
    if (this.domain) {
      this.refresh();
    }
  }

  refresh() {
    this.ontologyService.getProperties(this.domain).then(http => {
      let treeNode = [];
      let first = true;
      for (let i in http.response.result) {
        let resourceTree: ResourceTreeDTO = http.response.result[i];
        let node = this.dtoToNode(resourceTree);
        treeNode.push(node);

        this.nodes = treeNode;
      }
    });
  }

  displayPropertyNodeDetail(node) {
    this.displayPropertyDetail(node.data.uri, node.data.type);
  }

  displayPropertyDetail(uri, type) {
    this.ontologyService.getProperty(uri, type).then(http => {
      this.selected = http.response.result;
    });
  }

  getPropertyIcon(node) {
    if (OWL.isDatatypeProperty(node.data.type)) {
      return "fa#database";
    } else {
      return "fa#link";
    }
  }

  private dtoToNode(dto: ResourceTreeDTO) {
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
      isSelected: this.selected && this.selected.uri == dto.uri,
      isDraggable: false,
      isSelectable: !dto.disabled
    };
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
</i18n>
