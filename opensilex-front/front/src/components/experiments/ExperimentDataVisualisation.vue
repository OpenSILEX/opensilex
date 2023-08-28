<template>
  <div>
    <div>
      <opensilex-ExperimentDataVisualisationView
        :soFilter="soFilter" 
        @graphicCreated="onGraphicCreated"
        :selectedScientificObjects="selectedScientificObjects"
        :elementName="elementName"
      ></opensilex-ExperimentDataVisualisationView> 
    </div>     
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProvenanceGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { ScientificObjectsService } from "opensilex-core/index";
import { VariablesService } from "opensilex-core/index";
import TreeViewAsync from "../common/views/TreeViewAsync.vue";

@Component
export default class ExperimentDataVisualisation extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $t: any;
  uri = null;
  soService: ScientificObjectsService;
  varService: VariablesService;
  visibleDetails: boolean = false;
  searchVisible: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  showDataVisuView = false;
  numberOfSelectedRows = 0;
  refreshKey = 0;

  @Prop()
  elementName;

  refreshTypeSelectorComponent(){
    this.refreshKey += 1
  }

  filter = {
    start_date: null,
    end_date: null,
    provenance: null,
    variables: [],
    experiments: [this.uri],
    scientificObjects: [],
    targets: []
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

  @Ref("dataList") readonly dataList!: any;
  @Ref("dataForm") readonly dataForm!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("soSelector") readonly soSelector!: any;
  @Ref("soForm") readonly soForm!: any;
  @Ref("soTree") readonly soTree!: TreeViewAsync;

  get customColumns() {
    return [
      {
        id: "geometry",
        name: this.$t("ExperimentScientificObjects.geometry-label"),
        type: "WKT",
        comment: this.$t("ExperimentScientificObjects.geometry-comment"),
        is_required: false,
        is_list: false
      }
    ];
  }

    get credentials() {
        return this.$store.state.credentials;
    }

    get user() {
        return this.$store.state.user;
    }

  get selectedScientificObjects() {
    this.showDataVisuView = true;
    return this.selectedObjects.map(objectUri => {
      return {
        uri: objectUri,
        name: this.namedObjectsArray[objectUri]
      }
    });
  }

  public nodes = [];

  public filters = {
    name: "",
    types: [],
    parent: undefined,
    factorLevels: []
  };

  public selected = null;

  selectedObjects = [];
  selectedVariables = [];
  namedObjectsArray = [];

  @Ref("facilitySelector") readonly facilitySelector!: any;
  @Ref("page") readonly page!: any;

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

    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.varService = this.$opensilex.getService(
      "opensilex.VariablesService"
      );
  }

  onGraphicCreated() {
    let that = this;
    setTimeout(function() {
      that.page.scrollIntoView({
        behavior: "smooth",
        block: "end",
        inline: "nearest"
      });
    }, 500);
  }

  resetSearch() {
    this.resetFilters();
    this.refresh();
  }

  clear() {
    this.searchVisible = false;
    this.selectedProvenance = null;
    this.resetFilters();
    this.refresh();
  }

  refreshProvComponent() {
    this.refreshKey += 1;
  }


  resetFilters() {
    this.filter = {
    start_date: null,
    end_date: null,
    provenance: null,
    variables: [],
    experiments: [this.uri],
    scientificObjects: [],
    targets: []
    };
  }

  refresh() {
    this.searchVisible = true;
    this.dataList.refresh();
    //remove experiments filter from URL
    this.$nextTick(() => {
    this.$opensilex.updateURLParameter("experiments", null, "");
    });
  }

  loadAllChildren(nodeURI,page,pageSize) {
    return this.soService.getScientificObjectsChildren(
      nodeURI,
      this.uri,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      page,
      pageSize
    );
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

  initFormData(form) {
    form.experiment = this.uri;
    return form;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }


  unselectRefresh() {
    this.selected = null;
    this.selectedObjects = []; // fix bug filtre/selection
    this.showDataVisuView = false; 
    this.refresh();
  }

  get lang() {
    return this.$store.state.lang;
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
searchMethod(nodeURI, page, pageSize) {

    let orderBy = ["name=asc"];
    if(this.filters.parent || this.filters.types.length !== 0 || this.filters.factorLevels.length !== 0||  this.filters.name.length !== 0) {
       return this.soService.searchScientificObjects(
        this.uri, // experiment uri?: string,
        this.filters.types, 
        this.filters.name, 
        this.filters.parent ? this.filters.parent : nodeURI, 
        undefined, // Germplasm
        this.filters.factorLevels, 
        undefined, // facility?: string,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize );

    } else {

        return this.soService.getScientificObjectsChildren(
        nodeURI,
        this.uri,
        undefined,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize );
    }
  }

  searchParents(query, page, pageSize) {
    return this.soService
      .searchScientificObjects(
        this.uri, // experiment uri?: string,
        undefined, // rdfTypes?: Array<string>,
        query, // pattern?: string,
        undefined, // parentURI?: string,
        undefined, // Germplasm
        undefined, // factorLevels?: Array<string>,
        undefined, // facility?: string,
        undefined,
        undefined,
          undefined,
        [], // orderBy?: ,
        page, // page?: number,
        pageSize // pageSize?: number
      )
      .then(http => {
        let nodeList = [];
        for (let so of http.response.result) {
          nodeList.push({
            id: so.uri,
            label: so.name + " (" + so.rdf_type_name + ")"
          });
        }
        http.response.result = nodeList;
        return http;
      });
  }

  initForm() {
    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: this.selectedObjects,
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
    }
  }
}
</script>

<style scoped lang="scss">
.selection-box {
  margin-top: 1px;
  margin-left: 24px;
}

.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}

.card-header {
  padding-top: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

.card-header .badge {
  margin-left: 5px;
}

.scientificObjectsCards {
  display:contents
}
</style>
