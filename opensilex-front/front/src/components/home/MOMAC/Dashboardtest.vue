<template>
  <div id="map">
    <!--------------------------- MODALS ------------------------------->
    <!-- print map modal -->
    <b-modal id="modal-print-map">
      <template #default>
        <p>{{ $t("MapView.save-confirmation") }}</p>
      </template>
      <template #modal-footer>
        <b-button variant="danger" @click="savePDF(titleFile)">PDF</b-button>
        <b-button variant="info" @click="savePNG(titleFile)">PNG</b-button>
      </template>
    </b-modal>

    <!-- update device modal-->
    <opensilex-ModalForm
      ref="deviceForm"
      component="opensilex-DeviceForm"
      editTitle="Device.update"
      icon="fa#sun"
      modalSize="lg"
    ></opensilex-ModalForm>
    <!-- create filter modal-->
    <opensilex-ModalForm
      v-if="!errorGeometry"
      ref="filterForm"
      component="opensilex-FilterMap"
      createTitle="Filter.add"
      editTitle="Filter.update"
      icon="fa#sun"
      modalSize="m"
      @onCreate="showFiltersDetails"
      successMessage="Filter.created"
    ></opensilex-ModalForm>
    <!-- export modal-->
    <opensilex-ExportShapeModalList
      ref="exportShapeModalList"
      @onValidate="downloadFeatures"
    ></opensilex-ExportShapeModalList>
    <!-- chart modal -->
    <b-modal id="modal-chart" size="xl" hide-footer>
      <template #modal-title>
        <i class="ik ik-search mr-1"></i>
        {{ $t("component.project.filter-description") }}
      </template>
      <opensilex-ExperimentDataVisualisationView
        :soFilter="soFilter"
        :selected="selectedOS"
        :mapMode="mapMode"
        :soWithLabels="soWithLabels"
        :elementName="experimentData.name"
      ></opensilex-ExperimentDataVisualisationView>
    </b-modal>

    <!--------------------------- TOOLS BUTTONS ------------------------------->
    <div v-if="!editingMode" id="selected" class="d-flex">
      <div class="mr-auto p-2">
        <!-- help button -->
        <opensilex-HelpButton
          class="helpButton"
          label="component.common.help-button"
          @click="showInstructionMap = true"
        ></opensilex-HelpButton>

        <!-- map panel button (toggle side bar) -->
        <b-button v-b-toggle.map-sidebar>{{ $t("MapView.mapPanel") }}</b-button>
        <!-- focus button -->
        <opensilex-Button
          icon="fa#crosshairs"
          label="MapView.center"
          @click="defineCenter"
        ></opensilex-Button>
        <!-- print map button -->
        <opensilex-Button
          icon="fa#print"
          label="MapView.print"
          @click="printMap"
        ></opensilex-Button>
        <!-- export button -->
        <opensilex-Button
          icon="fa#download"
          label="MapView.export"
          @click="exportMap"
        ></opensilex-Button>
        <!-- time line button -->
        <opensilex-Button
          icon="fa#stopwatch"
          label="MapView.dateRange"
          @click="handleDateRangeStatus"
        ></opensilex-Button>
        <!-- chart button -->
        <opensilex-Button
          icon="fa#chart-area"
          label="MapView.chart"
          @click="showChart"
          :disabled="selectedOS.length === 0 || selectedOS.length > 15 ? true : false"
        ></opensilex-Button>
        <div>
          <br />
          <b-alert v-model="showInstructionMap" dismissible>
            <p v-html="$t('MapView.Instruction')"></p>
          </b-alert>
        </div>
      </div>
    </div>

    <!------------------------------------- MAP -------------------------------->
    <!--If editing mode = true >grey frame -->
    <div id="mapPoster" :class="editingMode ? 'bg-light border border-secondary' : ''">
      <div>
        <button @click="updateSelectionFeatures">Toggle Menu</button>
        <transition name="slide">
          <div v-show="menuVisible" class="menu">
            <!-- Menu content goes here -->
          </div>
        </transition>
      </div>
      <!-- Map config - "mapControls" to display the scale -->
      <vl-map
        ref="map"
        :default-controls="mapControls"
        :load-tiles-while-animating="true"
        :load-tiles-while-interacting="true"
        class="map"
        data-projection="EPSG:4326"
        style="height: 700px"
        @created="mapCreated"
        @pointermove="onMapPointerMove"
        @click="showDetails"
      >
        <!-- Zoom and position-->
        <vl-view
          ref="mapView"
          :min-zoom="2"
          :zoom="3"
          @update:center="overlayPositionsRecovery"
        ></vl-view>
        <!-- Base tile -->
        <vl-layer-tile id="osm">
          <vl-source-osm />
        </vl-layer-tile>
        <!-- Name Pop-up -->
        <!-- position ternary to show the overlay only if mouse on Object else default value -->
        <vl-layer-vector render-mode="image">
          <vl-source-vector :features="facilitiesData" :wrap-x="true">
            <vl-style-box>
              <vl-style-stroke color="blue" :width="2"></vl-style-stroke>
              <vl-style-fill color="rgba(0,   0,   255,   0.1)"></vl-style-fill>
            </vl-style-box>
          </vl-source-vector>
        </vl-layer-vector>

        <!-- Interaction for Selecting Vector Features -->

        <!-- stop-event property to be sure the map is not glitching vertically -->
        <vl-overlay
          id="overlay"
          :position="overlayCoordinate.length === 2 ? overlayCoordinate : [0, 0]"
          :stop-event="!!(selectPointerMove.name && selectPointerMove.type)"
        >
          <template v-slot="scope">
            <div class="panel-content">{{ displayInfoInOverlay() }}</div>
          </template>
        </vl-overlay>
        <!-- Interaction for selecting vector features -->
        <vl-interaction-select
          v-if="!editingMode"
          id="select"
          :features="selectedFeatures"
          @update:features="updateSelectionFeatures"
        />
      </vl-map>
    </div>

    <!--------------------- EVENT SIDEBAR ----------------------------->
    <b-sidebar
      id="event-sidebar"
      v-model="timelineSidebarVisibility"
      width="400px"
      right
      class="sidebar-content"
      no-header
      shadow
    >
      <template #default="{ hide }">
        <div
          class="b-sidebar-header header-brand hamburger-container opensilex-sidebar-header"
        >
          <div class="d-flex">
            <span id="map-sidebar___title__" class="text mr-auto p-3">
              {{ $t("MapView.eventPanelTitle").toUpperCase() }}
              &nbsp;
              <font-awesome-icon
                icon="question-circle"
                v-b-tooltip.hover.top="$t('MapView.eventPanel-help')"
              />
            </span>
            <button class="hamburger hamburger--collapse is-active p-3" @click="hide">
              <span class="hamburger-box">
                <span class="hamburger-inner"></span>
              </span>
            </button>
          </div>
        </div>
      </template>
    </b-sidebar>

    <!---------------------------- MENU SIDEBAR ----------------------------->
    <b-sidebar
      id="map-sidebar"
      visible
      no-header
      class="sidebar-content"
      ref="mapSidebar"
    >
      <template #default="{ hide }">
        <div
          class="b-sidebar-header header-brand hamburger-container opensilex-sidebar-header"
        >
          <div class="d-flex">
            <span id="map-sidebar___title__" class="text mr-auto p-3">{{
              $t("MapView.mapPanelTitle").toUpperCase()
            }}</span>
            <button class="hamburger hamburger--collapse is-active p-3" @click="hide">
              <span class="hamburger-box">
                <span class="hamburger-inner"></span>
              </span>
            </button>
          </div>
        </div>
        <b-tabs content-class="mt-3">
          <!-- A GARDER POUR DISPLAY D'AUTRES METADONNEES -->
          <!-- DEVICES -->
          <opensilex-TreeView
            :nodes.sync="devices"
            :class="isDisabled ? 'disabledMenu' : ''"
          >
            <template v-slot:node="{ node }">
              <span class="item-icon"> </span>&nbsp;
              <span v-if="node.title === deviceLabel"
                >{{ $t("MapView.mapPanelDevices") }} ({{
                  isDisabled
                    ? $t("MapView.mapPanelNotVisible")
                    : featuresDevice.flat().length
                }})</span
              >
              <span v-else
                >{{ nameType(node.title) }} ({{
                  getNumberByType(node.title, featuresDevice)
                }})</span
              >
            </template>

            <template v-slot:buttons="{ node }" v-if="!isDisabled">
              <opensilex-CheckboxForm
                v-if="node.title === deviceLabel"
                :value.sync="displayDevices"
                class="col-lg-2"
                :small="true"
                @update:value="updateVisibility(node, $event, devices, vectorLayerDevice)"
              ></opensilex-CheckboxForm>
              <opensilex-CheckboxForm
                :value="true"
                :disabled="!displayDevices"
                v-if="node.title !== deviceLabel"
                class="col-lg-2"
                :small="true"
                @update:value="updateVisibility(node, $event, devices, vectorLayerDevice)"
              ></opensilex-CheckboxForm>
            </template>
          </opensilex-TreeView>
          <!-- FILTERS -->
          <opensilex-TreeView :nodes.sync="filters">
            <template v-slot:node="{ node }">
              <span class="item-icon"> </span>&nbsp;
              <span v-if="node.title === 'Filters'"
                >{{ $t("MapView.mapPanelFilters") }} ({{ tabLayer.length }})</span
              >
              <span class="p-2 bd-highlight" v-else>{{ node.title }}</span>
            </template>

            <template v-slot:buttons="{ node }">
              <opensilex-CheckboxForm
                :disabled="!tabLayer.length"
                v-if="node.title === 'Filters'"
                :value.sync="displayFilters"
                class="col-lg-2"
                :small="true"
              ></opensilex-CheckboxForm>
              <div class="d-flex flex-row mx-auto" v-if="node.title !== 'Filters'">
                <opensilex-CheckboxForm
                  v-if="node.title !== 'Filters'"
                  :value="true"
                  @update:value="updateFilterDisplay(node)"
                  class="align-self-center"
                  :small="true"
                  :disabled="!displayFilters"
                ></opensilex-CheckboxForm>
                <opensilex-InputForm
                  style="width: 35px !important"
                  v-if="node.data.vlStyleStrokeColor"
                  type="color"
                  :value.sync="node.data.vlStyleStrokeColor"
                  @update:value="updateColorFilter(node, 'vlStyleStrokeColor')"
                  class="align-self-center"
                ></opensilex-InputForm>
                <opensilex-InputForm
                  style="width: 35px !important"
                  v-if="node.data.vlStyleFillColor"
                  type="color"
                  :value.sync="node.data.vlStyleFillColor"
                  @update:value="updateColorFilter(node, 'vlStyleFillColor')"
                  class="align-self-center"
                ></opensilex-InputForm>
                <opensilex-DeleteButton
                  label="FilterMap.filter.delete-button"
                  :small="true"
                  @click="
                    tabLayer.forEach((element, index) => {
                      if (element.titleDisplay === node.data.titleDisplay)
                        tabLayer.splice(index, 1);
                    })
                  "
                  class="align-self-center"
                ></opensilex-DeleteButton>
              </div>
            </template>
          </opensilex-TreeView>
          <!-- Create filter button -->
          <opensilex-CreateButton
            class="ml-50 mt-10"
            label="MapView.create-filter"
            @click="filterForm.showCreateForm()"
          ></opensilex-CreateButton>
        </b-tabs>
      </template>
    </b-sidebar>

    <!--------------------- LEGEND ----------------------------->
    {{ $t("MapView.Legend") }}:
    <span id="OS">{{ $t("MapView.LegendSO") }}</span>
    &nbsp;-&nbsp;
    <span id="StructuralArea">{{ $t("MapView.LegendStructuralArea") }}</span>
    &nbsp;-&nbsp;
    <span id="TemporalArea">{{ $t("MapView.LegendTemporalArea") }}</span>

    <!--------------------- DATE RANGE ----------------------------->
    <div class="timeline-slider" v-if="displayDateRange">
      <JqxRangeSelector
        ref="JqxRangeSelector"
        class="mx-auto"
        width="75%"
        padding="35px"
        height="15"
        :min="minDate"
        :max="maxDate"
        :range="range"
        :labelsOnTicks="false"
        :majorTicksInterval="majorTicksInterval"
        :minorTicksInterval="minorTicksInterval"
        :labelsFormat="labelsFormat"
        :markersFormatFunction="markersDateRangeFormat"
        theme="dark"
        @change="onChangeDateRange($event)"
      >
      </JqxRangeSelector>
    </div>

    <!--------------------- TABLE ----------------------------->
    <div
      id="selectedTable"
      v-if="selectedFeatures.length !== 0"
      class="selected-features"
    >
      <opensilex-TableView
        v-if="selectedFeatures.length !== 0"
        :fields="fieldsSelected"
        :items="selectedFeatures"
      >
        <template v-slot:cell(name)="{ data }">
          <opensilex-UriLink
            :to="customURIPath(data)"
            :uri="data.item.properties.uri"
            :value="data.item.properties.name"
            target="_blank"
          ></opensilex-UriLink>
        </template>

        <template v-slot:cell(type)="{ data }">{{
          nameType(data.item.properties.type)
        }}</template>

        <template v-slot:row-details="{ data }">
          <opensilex-DisplayInformationAboutItem
            :experiment="experiment"
            :item="data.item"
          />
        </template>

        <template v-slot:cell(actions)="{ data }">
          <b-button-group size="sm">
            <opensilex-DetailButton
              :detailVisible="data['detailsShowing']"
              :small="true"
              label="MapView.details"
              @click="showDetails(data)"
            ></opensilex-DetailButton>

            <opensilex-EditButton
              v-if="customCredential(data)"
              :small="true"
              label="MapView.update"
              @click="edit(data)"
            ></opensilex-EditButton>

            <opensilex-DeleteButton
              v-if="customCredential(data)"
              label="MapView.delete-button"
              @click="
                selectedFeatures.splice(selectedFeatures.indexOf(data.item), 1) &&
                  deleteItem(data)
              "
            ></opensilex-DeleteButton>
          </b-button-group>
        </template>
      </opensilex-TableView>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import Polygon from "ol/geom/Polygon";
import { Circle as CircleStyle, Fill, Stroke, Style, Text } from "ol/style";
import { DragBox } from "ol/interaction";
import * as olExtent from "ol/extent";
import Point from "ol/geom/Point";
import { platformModifierKeyOnly } from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
import GeoJSONFeature from "vuelayers/src/ol-ext/format";
import { toLonLat } from "ol/proj";
import { toStringHDMS } from "ol/coordinate";
import {
  PositionsService,
  DevicesService,
  ExperimentsService,
  OrganizationsService,
  EventsService,
  ExperimentGetDTO,
  OrganizationGetDTO,
  ResourceTreeDTO,
  DeviceGetDTO,
  OntologyService,
  FacilityGetDTO,
  DataService,
  TargetPositionCreationDTO,
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { transformExtent } from "vuelayers/src/ol-ext/proj";
import { defaults, ScaleLine } from "ol/control";
import GeoJSON from "ol/format/GeoJSON";
import Cluster from "ol/source/Cluster";
import VectorLayer from "ol/layer/Vector";
import VectorSource from "ol/source/Vector";
import Oeso from "../../ontologies/Oeso";
import Oeev from "../../ontologies/Oeev";
import * as turf from "@turf/turf";
import { jsPDF } from "jspdf";
import { saveAs } from "file-saver";
import { Store } from "vuex";
import VueI18n from "vue-i18n";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import ExperimentDataVisualisation from "../experiments/ExperimentDataVisualisation.vue";
import { each } from "highcharts";
import LineString from "ol/geom/LineString.js";

interface feature {
  type: string;
  id: string;
  geometry: {
    type: string;
    coordinates: any[][];
  };
}

@Component({
  components: { ExperimentDataVisualisation },
})
export default class MapView extends Vue {
  @Ref("JqxRangeSelector") readonly rangeSelector: any;
  @Ref("mapView") readonly mapView!: any;
  @Ref("map") readonly map!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("clusterSource") readonly clusterSource!: any;
  @Ref("vectorLayerDevice") readonly vectorLayerDevice!: any;
  @Ref("areaForm") readonly areaForm!: any;
  @Ref("filterForm") readonly filterForm!: any;
  @Ref("soForm") readonly soForm!: any;
  @Ref("deviceForm") readonly deviceForm!: any;
  @Ref("exportShapeModalList") readonly exportShapeModalList!: any;
  @Ref("mapSidebar") readonly mapSidebar!: any;

  ///////////// BASE DATA ////////////
  $opensilex: OpenSilexVuePlugin;
  $store: Store<any>;
  $t: any;
  $i18n: VueI18n;
  $bvModal: any;
  experimentData: ExperimentGetDTO = {
    name: null,
    start_date: null,
    end_date: null,
  };
  organizationData: OrganizationGetDTO = {
    name: null,
    publisher: {},
    facilities: [],
    sites: [],
  };
  facilitiesData: feature[] = [];

  private experiment: string = "";
  private organization: string = "";
  private experimentService: ExperimentsService;
  private organizationService: OrganizationsService;
  private ontologyService: OntologyService;
  private positionService: PositionsService;
  private eventsService: EventsService;
  private devicesService: DevicesService;
  private dataService: DataService;
  private editingMode: boolean = false;
  private typeLabel: { uri: string; name: string }[] = [];
  private lang: string;
  private langUnwatcher;

  ///////////// MAP PANEL ////////////
  isDisabled: boolean = true;

  private displayFilters: boolean = true;
  private devices: any[] = this.initDevices();
  private readonly deviceLabel: string = "Devices";
  private displayDevices: boolean = true;
  soFilter: any = {
    name: "",
    experiment: undefined,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  };
  selectedOS: string[] = [];

  ///////////// FEATURES DATA ////////////
  private endReceipt: boolean = false;
  //OS
  featuresOS: GeoJSONFeature[][] = [];
  private callSO: boolean = false;
  checkZoom: boolean = true;
  opacityOS: number = 0;

  //DEVICES
  featuresDevice: GeoJSONFeature[] = [];
  //FILTERS
  tabLayer: any[] = [];

  ///////////// MAP ////////////
  el: "map";
  selectPointerMove: { name: string; type: string } = {
    name: null,
    type: null,
  };
  overlayCoordinate: any[] = [];
  centerMap: any[] = [];
  private mapControls = defaults().extend([new ScaleLine()]);
  selectedFeatures: any[] = [];

  ///////////// TABLE ////////////
  fieldsSelected = [
    {
      key: "name",
      label: "MapView.name",
    },
    {
      key: "type",
      label: "MapView.type",
    },
    {
      key: "actions",
      label: "actions",
    },
  ];
  private detailsSO: boolean = false;

  ///////////// TOOLS BUTTONS ////////////
  showInstructionMap: boolean = false;

  ///////////// EVENT PANEL ////////////
  timelineSidebarVisibility: boolean = false;

  ///////////// DATE RANGE ////////////
  private displayDateRange: boolean = false;
  minDate: Date = null;
  maxDate: Date = null;
  majorTicksInterval = "day";
  labelsFormat = "yyyy";
  minorTicksInterval = "hour";
  range: { from: Date; to: Date } = { from: null, to: null };
  // filter: any = {};

  ///////////// MODAL ////////////
  private errorGeometry: boolean = false;
  exportedOS = [];
  exportedDevices = [];
  mapMode: boolean = true;
  soWithLabels: any[] = [];

  ///////////// TEST /////////////

  menuVisible: boolean = false;

  ///////////// BASE METHODS ////////////
  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get isMapHasLayer() {
    return this.featuresDevice.length > 0;
  }

  getType(type: string) {
    const res = new URL(type);
    if (res.hash != "") {
      // long type (example: http://opensilex.dev/set/area#youpi)
      return res.hash;
    } else {
      // short type (example: Vocabulary#Area)
      return res.pathname;
    }
  }

  created() {
    this.$opensilex.showLoader();

    this.positionService = this.$opensilex.getService("opensilex.PositionsService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
    this.devicesService = this.$opensilex.getService("opensilex.DevicesService");
    this.experimentService = this.$opensilex.getService("opensilex.ExperimentsService");
    this.organizationService = this.$opensilex.getService(
      "opensilex.OrganizationsService"
    );
    this.dataService = this.$opensilex.getService("opensilex.DataService");

    // this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
    this.experiment = decodeURIComponent(this.$route.params.uri);

    this.experimentService
      .getExperiment(this.experiment)
      .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
        this.experimentData = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
    this.retrievesNameOfType();
    this.soFilter = {
      name: "",
      experiment: this.experiment,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };
    this.getOrga();
    console.log("FACILITY :", this.facilitiesData);
  }

  createPolygonFeature(facility) {
    const feature = {
      type: facility.geometry.type,
      id: facility.uri,
      geometry: {
        type: facility.geometry.geometry.type,
        coordinates: facility.geometry.geometry.coordinates,
      },
    };

    // console.log("COORDINATES: ", facility.geometry.geometry.coordinates);

    return feature;
  }

  // Fill the facilitiesData array
  async fillFacilitiesArray(uri) {
    try {
      const facility = await this.organizationService.getFacility(uri);
      const polygonFeature = this.createPolygonFeature(facility.response.result);

      this.facilitiesData.push(polygonFeature);
      // console.log("Facility data:", facility.response.result);
    } catch (error) {
      console.error("Error fetching facility data:", error);
    }
  }

  async getOrga() {
    try {
      const facilities = await this.organizationService.getAllFacilities();
      console.log(facilities);
      for (const facility of facilities.response.result) {
        await this.fillFacilitiesArray(facility.uri);
      }
      return this.facilitiesData;
    } catch (error) {
      console.error("Error fetching all facilities:", error);
    }
  }

  retrievesNameOfType() {
    let typeLabel: { uri: string; name: string }[] = [];
    let baseTypes = [Oeso.AREA_TYPE_URI, Oeso.DEVICE_TYPE_URI, Oeev.EVENT_TYPE_URI];

    this.lang = this.user.locale;
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");

    //get the label for the type Device
    typeLabel.push({
      uri: this.$opensilex.getShortUri(Oeso.DEVICE_TYPE_URI),
      name: this.$i18n.t("Device.title").toString(),
    });

    baseTypes.forEach((baseType) => {
      this.ontologyService
        .getSubClassesOf(baseType, true)
        .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
          const res = http.response.result;
          this.extracted(res, typeLabel);
        })
        .catch(this.$opensilex.errorHandler);
      this.typeLabel = typeLabel;
    });
  }

  nameType(uriType): string {
    if (this.user.locale != this.lang) {
      this.retrievesNameOfType();
    }

    for (let typeLabelElement of this.typeLabel) {
      if (typeLabelElement.uri == uriType) {
        return typeLabelElement.name;
      }
    }
  }

  private extracted(
    res: Array<ResourceTreeDTO>,
    typeLabel: { uri: string; name: string }[]
  ) {
    res.forEach(({ name, uri, children }) => {
      typeLabel.push({
        uri: uri,
        name: name.substr(0, 1).toUpperCase() + name.substr(1),
      });
      if (children.length > 0) {
        this.extracted(children, typeLabel);
      }
    });
  }

  removeFromFeatures(uri, features) {
    features.splice(
      features.findIndex((feature) => feature.properties.uri === uri),
      1
    );
    return features;
  }

  ///////////// MAP METHODS ////////////
  //show Name pop-up
  displayInfoInOverlay(): string {
    if (this.selectPointerMove.name && this.selectPointerMove.type) {
      // if Object existing and filled
      return this.selectPointerMove.name + " (" + this.selectPointerMove.type + ")";
    } else {
      return "";
    }
  }
  // Gets the name when the cursor hovers over the item.
  onMapPointerMove({ pixel }: any) {
    const hitFeature = this.map.forEachFeatureAtPixel(pixel, (feature) => feature);
    if (hitFeature) {
      this.selectPointerMove = {
        name: hitFeature.values_.name,
        type: this.nameType(hitFeature.values_.rdf_type)
          ? this.nameType(hitFeature.values_.rdf_type)
          : this.nameType(hitFeature.values_.type),
      };
    } else {
      this.selectPointerMove = { name: null, type: null };
    }
  }
  // Used to display details on the map and in the table
  showDetails(data, isMap = false) {
    console.log("DATA: ", data);
    if (isMap) {
      let uriResult = data.properties.uri;
    } else {
      if (!data.detailsShowing) {
        let uriResult = data.item.properties.uri;
      }
      data.toggleDetails = -data.toggleDetails();
    }
  }
  // Select multi-features (OS, Areas, devices and Filters)
  private multiSelect(map) {
    // a DragBox interaction used to select features by drawing boxes
    const dragBox = new DragBox({
      condition: platformModifierKeyOnly,
      onBoxEnd: () => {
        // features that intersect the box are selected
        if (this.isMapHasLayer) {
          const extent = dragBox.getGeometry().getExtent();
          //All layers (tile/vector)
          var layers = this.map.$map.getLayers();
          //Recover vector and visible layers
          layers.forEach((layer) => {
            if (layer.getVisible() && layer.type === "VECTOR") {
              const source = layer.getSource();
              source.forEachFeatureIntersectingExtent(extent, (feature: any) => {
                feature = olExt.writeGeoJsonFeature(feature);
                this.selectedFeatures.push(feature);
              });
            }
          });
        }
      },
    });

    map.$map.addInteraction(dragBox);

    this.map.$map.on("click", (event) => {
      let isFeatureSelected = this.map.$map.getFeaturesAtPixel(event.pixel);
      if (!isFeatureSelected && !this.editingMode) {
        this.selectedFeatures = [];
        this.selectedOS = [];
        this.soWithLabels = [];
      }
    });

    // clear selection when drawing a new box and when clicking on the map
    dragBox.on("boxstart", () => {
      this.selectedFeatures = [];
      this.selectedOS = [];
      this.soWithLabels = [];
    });
  }

  mapCreated(map) {
    this.multiSelect(map);
  }

  //Focus on map vectors
  defineCenter() {
    if (this.featuresOS.length > 0 && this.vectorSource[0].$source) {
      let extent = olExtent.createEmpty();
      setTimeout(() => {
        this.vectorSource.forEach((v) => {
          if (v && v.$source) {
            let extentTemporary = v.$source.getExtent();
            olExtent.extend(extent, extentTemporary);
          }
        });
        this.mapView.$view.fit(extent);
        // create cluster after
        this.getClusterFeatures();
      }, 200);
    }
  }

  select(value) {
    this.$emit("select", value);
  }

  updateSelectionFeatures(features) {
    console.log("CLICKED: ", features);
    this.menuVisible = !this.menuVisible;
  }
  //Config Mapview position
  private overlayPositionsRecovery() {
    let coordinateExtent = this.getCoordinateExtent();

    this.calcOverlayCoordinate(coordinateExtent);

    this.calcCenterMap(coordinateExtent);
  }
  // Change the projection
  private getCoordinateExtent() {
    return transformExtent(
      this.mapView.$view.calculateExtent(),
      "EPSG:3857",
      "EPSG:4326"
    );
  }
  //To show "name feature" pop-up in the upper left corner
  private calcOverlayCoordinate(coordinateExtent) {
    this.overlayCoordinate = [
      coordinateExtent[0] + (coordinateExtent[2] - coordinateExtent[0]) * 0.0303111,
      coordinateExtent[3] + (coordinateExtent[1] - coordinateExtent[3]) * 0.025747,
    ];
  }

  private calcCenterMap(coordinateExtent) {
    this.centerMap = [
      (coordinateExtent[0] + coordinateExtent[2]) / 2,
      (coordinateExtent[1] + coordinateExtent[3]) / 2,
    ];
  }

  //Show areas and devices only under zoom 9 and get the current map expansion

  //Display chart
  showChart() {
    this.$bvModal.show("modal-chart");
    this.mapSidebar.hide();
  }

  //get visible OS features to build the cluster
  getClusterFeatures() {
    if (this.vectorSource === undefined) {
      return;
    } else {
      this.clusterSource.$source.clear();
      this.$opensilex.showLoader();

      let features = [];
      const isVectorSourceMounted = (vector) =>
        vector && vector.getFeatures() && vector.getFeatures().length > 0;

      if (
        this.vectorSource.length === this.featuresOS.length &&
        this.vectorSource.every(isVectorSourceMounted)
      ) {
        this.vectorSource.forEach((vector) => {
          if (vector.$parent.$layer.getVisible()) {
            features.push(vector.getFeatures());
          }
        });
        this.$opensilex.hideLoader();
        console.log(new Date());
        this.clusterSource.$source.addFeatures(features.flat());
        console.log(new Date());
        console.trace();
      }
    }
  }

  // manage behavior on click on cluster point -> zoom in
  manageCluster(e) {
    this.map.forEachFeatureAtPixel(e.pixel, (feature) => {
      //transform all geometries into points and build the new extent
      if (feature.get("features")) {
        let points = [];
        let features = feature.get("features");

        features.forEach((feat) => {
          let geom = feat.getGeometry();
          if (geom.getType() == "Point") {
            points.push(geom.getCoordinates());
          } else if (geom.getType() == "Polygon") {
            points.push(geom.getInteriorPoint().getCoordinates());
          } else if (geom.getType() == "LineString") {
            let point = new Point(geom.getCoordinateAt(0.5), undefined);
            points.push(point.getCoordinates());
          }
        });

        this.mapView.$view.fit(olExtent.boundingExtent(points), { maxZoom: 17 });
      }
    });
  }

  // cluster points style
  makeClusterStyleFunc() {
    const styleCache = {};

    return function __clusterStyleFunc(feature) {
      const size = feature.get("features").length;
      let style = styleCache[size];
      if (!style) {
        style = new Style({
          image: new CircleStyle({
            radius: 10,
            stroke: new Stroke({
              color: "#fff",
            }),
            fill: new Fill({
              color: "#00a38d",
            }),
          }),
          text: new Text({
            text: size.toString(),
            fill: new Fill({
              color: "#fff",
            }),
          }),
        });
        styleCache[size] = style;
      }
      return style;
    };
  }

  ///////////// FILTERS METHODS ////////////
  get filters() {
    let result = [];
    let filt = {
      title: "Filters",
      data: {
        isCheckable: true,
        name: "Filters",
        selected: false,
        disabled: false,
      },
      isLeaf: false,
      children: [],
      isExpanded: true,
      isSelected: null,
      isDraggable: false,
      isSelectable: false,
      isCheckable: true,
    };

    for (let filter of this.tabLayer) {
      let tmp: any = {
        title: filter.titleDisplay,
        isLeaf: true,
      };
      tmp.isSelectable = false;
      tmp.isDraggable = false;
      tmp.isCheckable = true;
      tmp.isExpanded = false;
      tmp.isSelected = null;
      tmp.data = filter;
      tmp.data.vlStyleFillColor = filter.vlStyleFillColor;
      filt.children.push(tmp);
    }
    result.push(filt);
    return result;
  }

  set filters(value) {}

  showFiltersDetails(filterResult: any) {
    this.$opensilex.showLoader();
    if (filterResult.ref) {
      this.tabLayer.forEach((element, index) => {
        if (element.ref === filterResult.ref) {
          this.tabLayer.splice(index, 1);
        }
      });
      this.tabLayer.push(filterResult);
    }
    this.$opensilex.hideLoader();
  }

  colorFeature(color) {
    return (
      "rgba(" +
      parseInt(color.slice(1, 3), 16) +
      "," +
      parseInt(color.slice(3, 5), 16) +
      "," +
      parseInt(color.slice(5, 7), 16) +
      ",0.5)"
    );
  }

  ///////////// DEVICES METHODS ////////////
  getDeviceDetails(listURI, listDevicePositions) {
    //get details of each device loaded
    this.devicesService
      .getDeviceByUris(listURI)
      .then((http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>) => {
        let res = http.response.result;
        listDevicePositions.forEach((devicePosition) => {
          let element = res.find((el) => el.uri === devicePosition.properties.uri);
          //add details device
          devicePosition.properties.details = element;
          //formatting for pop-up name and table
          devicePosition.properties.name = element.name;
          devicePosition.properties.type = element.rdf_type;
          // Check if already exists
          let bool = true;
          this.devices[0].children.forEach((children) => {
            // for (let children of this.devices[0].children) {
            if (children.title === element.rdf_type) {
              bool = false;
            }
          });
          if (bool) {
            this.devices[0].isExpanded = true;
            //formatting devices for the panel map
            let children = {
              title: element.rdf_type,
              children: [],
              isLeaf: true,
              isSelectable: false,
              isDraggable: false,
              isCheckable: true,
              isExpanded: false,
              isSelected: null,
              isVisible: true,
            };
            this.devices[0].children.push(children);
          }
          //Feature formatting for efficient display of vectors
          let inserted = false;
          this.featuresDevice.forEach((item) => {
            if (item[0].properties.type === devicePosition.properties.type) {
              item.push(devicePosition);
              inserted = true;
            }
          });
          if (!inserted) {
            this.featuresDevice.push([devicePosition]);
          }
        });
      })
      .finally(() => {
        setTimeout(() => {
          this.setVisibility(this.devices, this.vectorLayerDevice);
        }, 200);
      });
  }

  //Recovery devices in  the current map expansion
  private devicesRecovery(geometry) {
    this.featuresDevice = [];
    let listURIDevices = [];
    let listDevicePositions = [];

    this.positionService
      .searchGeospatializedPosition(
        geometry,
        Oeso.DEVICE_TYPE_URI,
        new Date(this.experimentData.start_date).toISOString(),
        this.experimentData.end_date
          ? new Date(this.experimentData.end_date).toISOString()
          : undefined,
        undefined,
        500
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<TargetPositionCreationDTO>>>) => {
        const res = http.response.result as any;
        res.forEach((element) => {
          //list URI target (Devices) from positions
          listURIDevices.push(element.targetPositions[0].target);
          //formatting Positions
          if (element.targetPositions[0].position.coordinates != null) {
            element = {
              geometry: {
                coordinates:
                  element.targetPositions[0].position.coordinates.coordinates.values,
                type: "Point",
              },
              type: "Feature",
              properties: {
                uri: element.targetPositions[0].target,
                nature: this.deviceLabel,
                event: element.uri,
              },
            };
            let bool = true;
            for (let device of listDevicePositions) {
              if (device.properties.uri === element.properties.uri) {
                // Check if already exists
                bool = false;
              }
            }
            if (bool) {
              listDevicePositions.push(element);
            }
          }
        });
      })
      .catch(this.$opensilex.errorHandler)
      .finally(() => {
        if (listDevicePositions.length === 0) {
          this.devices = this.initDevices();
        } else {
          this.getDeviceDetails(listURIDevices, listDevicePositions);
        }
        this.endReceipt = true;
        this.$opensilex.hideLoader();
      });
  }

  ///////////// SAVE MAP METHODS ////////////
  get titleFile(): String {
    return this.getType(this.$attrs.uri) + "-map";
  }
  //On click, open modal "print map"
  printMap() {
    this.$bvModal.show("modal-print-map");
  }
  //save and ddl the map in PNG format
  savePNG(titleFile: String) {
    let canvas = document.getElementsByTagName("canvas")[0];
    canvas.toBlob((blob) => {
      saveAs(blob, titleFile + ".png");
    });
    this.$bvModal.hide("modal-print-map");
  }
  //save and ddl the map in PDF format
  savePDF(titleFile: String) {
    let map = this.map.$map;
    let size = map.getSize();
    let dim = [size[0], size[1]];
    let resolution = 32;
    let width = Math.round((dim[0] * resolution) / 25.4);
    let height = Math.round((dim[1] * resolution) / 25.4);
    let extent = map.getView().calculateExtent(size);
    map.once("rendercomplete", (event) => {
      let canvas = event.context.canvas;
      let data = canvas.toDataURL("image/jpeg");
      let pdf = new jsPDF({
        orientation: "landscape",
        unit: "px",
        format: [dim[0], dim[1]],
      });
      pdf.addImage(data, "JPEG", 0, 0, dim[0], dim[1]);
      pdf.save(titleFile + ".pdf");
      this.$bvModal.hide("modal-print-map");
      map.setSize(size);
      map.getView().fit(extent, { size: size });
    });
    let printSize = [width, height];
    map.setSize(printSize);
  }
  //save and ddl the map in shape format
  exportMap() {
    // 1 - Defines features to export
    let exportedFeat = [];

    //if there is a selection, save it
    if (this.selectedFeatures.length > 0) {
      exportedFeat = this.selectedFeatures;
    }
    //without selection, save all visible objects
    else {
      this.map.$map.getLayers().forEach((layer) => {
        if (layer.getVisible() && layer.type === "VECTOR") {
          const source = layer.getSource();
          source.forEachFeature((feature: any) => {
            feature = olExt.writeGeoJsonFeature(feature);
            exportedFeat.push(feature);
          });
        }
      });
    }
    //Format exported features
    let exportedFeatures = exportedFeat.map(
      (feat) =>
        (feat = {
          name: feat.properties.name,
          uri: feat.properties.uri,
          rdf_type: feat.properties.type,
          geometry: feat.geometry,
          nature: feat.properties.nature,
        })
    );

    // 2 - Selected properties to export
    // TODO:  adding selected variable data - standby
    // Filter exportedFeatures by type

    this.exportedDevices = exportedFeatures.filter(
      (feature) => feature.nature === this.deviceLabel
    );

    if (this.exportedOS.length > 10000 || this.exportedDevices.length > 10000) {
      this.$opensilex.showErrorToast(this.$i18n.t("MapView.export-error").toString());
    } else {
      let disabled = { devices: false };

      if (this.exportedDevices.length === 0) {
        disabled.devices = true;
      }

      this.exportShapeModalList.show(disabled);
    }
  }

  downloadFeatures(values) {
    this.$opensilex.showInfoToast(this.$i18n.t("MapView.export-info").toString());

    if (this.exportedDevices.length > 0) {
      this.buildRequest(
        "/core/devices/export_geospatial",
        "export_devices" + "_" + values.format,
        { selected_props: values.devices.props, format: values.format },
        this.exportedDevices
      );
    }

    if (this.exportedOS.length === 0 && this.exportedDevices.length === 0) {
      this.$opensilex.showErrorToast(this.$i18n.t("MapView.export-no-found").toString());
    }
  }
  buildRequest(path: string, title: string, queryParams: any, body: any) {
    //Get service to export
    let today = new Date();
    let filename =
      title +
      "_" +
      this.$opensilex.$dateTimeFormatter.formatISODate(today) +
      "_" +
      today.getHours() +
      "H" +
      today.getHours();

    setTimeout(() => {
      this.$opensilex.downloadFilefromPostOrGetService(
        path,
        filename,
        "zip",
        "POST",
        queryParams,
        body
      );
    }, 1000);
  }

  ///////////// TABLE METHODS ////////////
  customURIPath(data) {
    let uri = data.item.properties.uri;
    switch (data.item.properties.nature) {
      default:
        return { path: "/scientific-objects/details/" + encodeURIComponent(uri) };

      case this.deviceLabel:
        return { path: "/device/details/" + encodeURIComponent(uri) };
    }
  }

  customCredential(data) {
    switch (data.item.properties.nature) {
      default:
        return this.user.hasCredential(
          this.credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
        );
      case this.deviceLabel:
        return this.user.hasCredential(
          this.credentials.CREDENTIAL_DEVICE_MODIFICATION_ID
        );
    }
  }

  deleteItem(data) {
    let uri = data.item.properties.uri;
    let option = {
      serviceRequest: null,
      title: null,
      updateFeatures: null,
    };

    switch (data.item.properties.nature) {
      default:
        option.serviceRequest = this.devicesService.deleteDevice(uri);
        option.title = "Device.title";
        option.updateFeatures = this.featuresDevice;
        break;
    }

    return option.serviceRequest
      .then((http) => {
        let message =
          this.$i18n.t(option.title) +
          " " +
          http.response.result +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        let updateList = this.removeFromFeatures(uri, option.updateFeatures.flat());
        //Feature formatting for efficient display of vectors
        let newFeatures = [];
        updateList.forEach((element) => {
          let inserted = false;
          newFeatures.forEach((item) => {
            if (item[0].properties.type === element.properties.type) {
              item.push(element);
              inserted = true;
            }
          });
          if (!inserted) {
            newFeatures.push([element]);
          }
        });
        switch (data.item.properties.nature) {
          default:
            this.featuresOS = newFeatures;
            break;

          case this.deviceLabel:
            this.featuresDevice = newFeatures;
            break;
        }
      })
      .catch((error) => {
        if (
          error.response.result.title &&
          error.response.result.title === "LINKED_DEVICE_ERROR"
        ) {
          let message =
            this.$i18n.t("DeviceList.associated-device-error") +
            " " +
            error.response.result.message;
          this.$opensilex.showErrorToast(message);
        } else {
          this.$opensilex.errorHandler(error);
        }
      })
      .finally(() => {
        this.$opensilex.hideLoader();
      });
  }
  //To update OS/areas
  private edit(data) {
    let uri = data.item.properties.uri;
    let form = data.item.properties;

    switch (data.item.properties.nature) {
      default:
        this.callSO = true;
        break;

      case this.deviceLabel:
        this.deviceForm.showEditForm(form.details);
        break;
    }
  }

  ///////////// MAP PANEL METHODS ////////////
  getNumberByType(type: String, features: any[]): Number {
    let res = 0;
    for (let layer of features.flat()) {
      if (layer.properties.type === type) {
        res += 1;
      }
    }
    return res;
  }

  updateFilterDisplay(node) {
    for (let layer of this.tabLayer) {
      if (layer.titleDisplay === node.title) {
        if (layer.display === "false") {
          layer.display = "true";
          node.data.display = "true";
        } else {
          layer.display = "false";
          node.data.display = "false";
        }
        return;
      }
    }
  }

  updateColorFilter(node: any, type: String) {
    this.tabLayer.forEach((layer) => {
      if (layer.titleDisplay === node.title) {
        if (type === "vlStyleStrokeColor") {
          layer.vlStyleStrokeColor = node.data.vlStyleStrokeColor;
        } else {
          layer.vlStyleFillColor = node.data.vlStyleFillColor;
        }
        return;
      }
    });
  }
  // Update the visibility of elements
  updateVisibility(node, event, treeView, layers) {
    //set checkbox status in treeView data
    if (treeView[0].title === node.title) {
      treeView[0].isVisible = event;
    } else {
      treeView[0].children.forEach((child) => {
        if (child.title === node.title) {
          child.isVisible = event;
        }
      });
    }
    this.setVisibility(treeView, layers);
  }

  //set visibility value from map panel to layers
  setVisibility(treeView, layers) {
    this.$opensilex.showLoader();
    if (layers) {
      if (!treeView[0].isVisible) {
        layers.forEach((layer) => {
          layer.$layer.setVisible(treeView.isVisible);
        });
      } else {
        treeView[0].children.forEach((child) => {
          let layerNode = layers.filter((layer) => {
            let features = layer.getSource().getFeatures();
            if (features[0].getProperties().type === child.title) {
              return layer;
            }
          });
          if (layerNode[0]) {
            layerNode[0].$layer.setVisible(child.isVisible);
          }
        });
      }
    }
    //update the OS cluster visibility
    if (this.checkZoom) {
      this.getClusterFeatures();
    }
    this.$opensilex.hideLoader();
  }

  initDevices() {
    return [
      {
        title: this.deviceLabel,
        children: [],
        isLeaf: false,
        isExpanded: false,
        isSelected: null,
        isDraggable: false,
        isSelectable: false,
        isCheckable: true,
        isVisible: true,
      },
    ];
  }

  ///////////// DATE RANGE METHODS ////////////
  onChangeDateRange(event) {
    this.range = { from: event.args.from, to: event.args.to };
    let startDate: string = this.formatDate(this.range.from);
    let endDate: string = this.formatDate(this.range.to);
  }

  formatDate(date): string {
    let d = new Date(date),
      month = String(d.getMonth() + 1).padStart(2, "0"),
      day = String(d.getDate()).padStart(2, "0"),
      year = d.getFullYear();
    return [year, month, day].join("-");
  }

  calcDifferenceDateInDays(from: Date, to: Date): number {
    let diffInTime = to.getTime() - from.getTime();
    let diffInDays = diffInTime / (1000 * 3600 * 24);

    return diffInDays;
  }
  majorTicksIntervalFct() {
    if (this.minDate || this.maxDate) {
      const from = this.minDate;
      const to = this.maxDate;
      if (this.calcDifferenceDateInDays(from, to) > 600) {
        return "year";
      } else if (this.calcDifferenceDateInDays(from, to) > 40) {
        return "month";
      } else {
        return "day";
      }
    } else {
      return "day";
    }
  }

  minorTicksIntervalFct() {
    if (this.minDate || this.maxDate) {
      const from = this.minDate;
      const to = this.maxDate;

      if (this.calcDifferenceDateInDays(from, to) > 600) {
        return "month";
      } else if (this.calcDifferenceDateInDays(from, to) > 40) {
        return "day";
      } else {
        return "hour";
      }
    } else {
      return "hour";
    }
  }

  labelsFormatFct() {
    if (this.minDate || this.maxDate) {
      const from = this.minDate;
      const to = this.maxDate;

      if (this.calcDifferenceDateInDays(from, to) > 600) {
        return "yyyy";
      } else if (this.calcDifferenceDateInDays(from, to) > 40) {
        return "MM";
      } else {
        return "dd";
      }
    } else {
      return "ddd";
    }
  }

  markersDateRangeFormat(value: string, position: string) {
    const date = new Date(value);
    let res: string = "";

    if (position == "left") res += this.$i18n.t("MapView.marketFrom");
    else res += this.$i18n.t("MapView.marketTo");
    res += ": ";
    if (this.$store.getters.language == "en") {
      res +=
        String(date.getMonth() + 1).padStart(2, "0") +
        "/" +
        String(date.getDate()).padStart(2, "0");
    } else {
      res +=
        String(date.getDate()).padStart(2, "0") +
        "/" +
        String(date.getMonth() + 1).padStart(2, "0");
    }
    res += "/" + date.getFullYear();
    return "<span>" + res + "<span>";
  }

  configDateRange(experiment) {
    let min = new Date(experiment.start_date);
    min.setHours(0, 0, 0, 0);

    let max;
    if (experiment.end_date) {
      max = new Date(experiment.end_date);
    } else {
      max = new Date();
    }

    max.setHours(0, 0, 0, 0);
    this.minDate = min;
    this.maxDate = max;
    this.range = { from: min, to: max };

    this.minorTicksInterval = this.minorTicksIntervalFct();
    this.majorTicksInterval = this.majorTicksIntervalFct();
    this.labelsFormat = this.labelsFormatFct();
  }
  //Show date range
  handleDateRangeStatus() {
    if (this.displayDateRange) {
      this.displayDateRange = !this.displayDateRange;
    } else {
      this.initDateRange();
    }
  }

  initDateRange() {
    // Recover start and end of the experiment
    if (!this.minDate || !this.maxDate || !this.range.from || !this.range.to) {
      this.getRangeDatesOfExperiment();
    } else {
      this.displayDateRange = !this.displayDateRange;
    }
  }

  getRangeDatesOfExperiment(): Promise<any> {
    return new Promise((resolve) => {
      this.experimentService
        .getExperiment(this.experiment)
        .then((http) => {
          let res = http.response.result;
          this.configDateRange(res);
          this.displayDateRange = !this.displayDateRange;

          // checkfilter
          // let from ;
          // if(!this.filter.from  ){
          //   from = this.minDate;
          // }else{
          //   from = this.filter.from
          // }
          // let to;
          // console.log(this.filter,!this.filter.to)
          // if(!this.filter.to  ){
          //   to = this.maxDate;
          // }else{
          //   to = this.filter.to
          // }

          resolve("");
        })
        .catch(this.$opensilex.errorHandler);
    });
  }

  ///////////// EVENT PANEL METHODS ////////////
  //select Features from timeline??
}
</script>

<style lang="scss" scoped>
@import "~jqwidgets-scripts/jqwidgets/styles/jqx.base.css";
@import "~jqwidgets-scripts/jqwidgets/styles/jqx.dark.css";

p {
  font-size: 115%;
  margin-top: 1em;
}

.border-secondary {
  border-width: 2mm !important;
}

#OS {
  color: blue;
}

#StructuralArea {
  color: green;
}

#TemporalArea {
  color: red;
}

.panel-content {
  background: white;
  box-shadow: 0 0.25em 0.5em transparentize(#000000, 0.8);
  border-radius: 5px;
  text-indent: 2px;
  max-width: 75%;
}

::v-deep .b-table-details .card {
  margin: 0;
  padding: 0;
  background-color: transparent;
  box-shadow: none;
}

::v-deep .jqx-scrollbar-state-normal-dark,
.jqx-grid-bottomright-dark,
.jqx-panel-bottomright-dark,
.jqx-listbox-bottomright-dark {
  background-color: transparent !important;
}

::v-deep .jqx-rangeselector-slider {
  background-color: #00a38d7e !important;
}

::v-deep .jqx-fill-state-normal-dark {
  background: #00a38d;
  border-color: #00a38d;
}

::v-deep .jqx-scrollbar-thumb-state-normal-dark,
.jqx-scrollbar-thumb-state-normal-horizontal-dark {
  background: transparent;
  border-color: transparent;
}

::v-deep .jqx-scrollbar-state-normal-dark,
.jqx-grid-bottomright-dark,
.jqx-panel-bottomright-dark,
.jqx-listbox-bottomright-dark {
  background-color: #3e3e42a8 !important;
  border: 1px solid #3e3e42a8;
  border-left-color: #3e3e42a8;
}

::v-deep .jqx-rangeselector-inner-slider {
  background: none;
}

.map {
  position: relative;
}

.map-panel {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
}

.b-sidebar-outer {
  z-index: 1045;
}

::v-deep .b-sidebar {
  width: 240px;
}

::v-deep div.b-sidebar-header {
  background-color: #00a38d;
  color: #ffffff;
  font-size: 1.25rem;
}

::v-deep .b-sidebar > .b-sidebar-body {
  background-color: white;
}

#selected > div > button {
  margin-right: 20px;
}

.custom-checkbox .custom-control-label {
  font-size: 13px;
}

.form-group {
  margin-bottom: 0;
}

.hamburger.is-active .hamburger-inner,
.hamburger.is-active .hamburger-inner::after,
.hamburger.is-active .hamburger-inner::before {
  background-color: #fff;
}

.hamburger-box {
  width: 30px;
}

.opensilex-sidebar-header {
  width: 99%;
  height: 60px;
}

.timeline-slider {
  padding-top: 10px;
}

.selected-features {
  animation-name: slide-up;
  animation-duration: 0.6s;
}

@keyframes slide-up {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.disabledMenu {
  color: grey;
  background-color: lightgrey;
  pointer-events: none;
}

.helpButton:hover {
  background-color: #00a28c;
  color: #f1f1f1;
}

.helpButton {
  margin-left: -5px;
  color: #00a28c;
  font-size: 1.2em;
  border: none;
}

.helpButton:hover {
  background-color: #00a28c;
  color: #f1f1f1;
}
.helpButton {
  margin-left: -5px;
  color: #00a28c;
  font-size: 1.2em;
  border: none;
}
</style>

<i18n>
en:
  MapView:
    name: name
    description: description
    type: type
    label: Geometry
    add-button: Add metadata
    add-area-button: Add area
    delete-button: Delete element
    selected-button: Exit creation mode
    errorLongitude: the longitude must be between -180 and 180
    errorLatitude: the latitude must be between -90 and 90
    Legend: Legend
    LegendSO: Scientific Object
    LegendStructuralArea: Structural Area
    LegendTemporalArea: Temporal Area
    Instruction: Press Shift to <b>select item by item</b> on the map.<br> Press and hold Shift + Alt + Click and move the mouse <b>to rotate</b> the map.<br> Press Ctrl + Click while dragging to <b>select multiple scientific objects</b>.<br>Press Shift + Click while dragging to define a <b>zoom-in area</b>.<br> To display a graph, select one to 15 scientific objects on the map and click on the "Display graphic" button.
    WarningInstruction: Currently, the selection tool does not follow the rotation.
    details: Show or hide element details
    author: Author
    update: Update element
    display: Layers
    displayFilter: Filters ({count})
    mapPanel: Manage map panel
    mapPanelTitle: Map Panel
    mapPanelNotVisible : not visible
    eventPanelTitle: Events Panel
    eventPanel-help: The event panel displays all the events linked to the temporal areas. They are ordered from most recent to oldest.
    mapPanelScientificObjects: Scientific Objects
    mapPanelAreas: Areas
    mapPanelAreasStructuralArea: Structural area
    mapPanelAreasTemporalArea: Temporal area
    mapPanelFilters: Filters
    mapPanelDevices : Devices
    create-filter: Create filter
    center: Refocus the map
    print: Print the map
    chart: Display graph - from 1 to 15 scientific objects
    time: See temporal(s) area(s)
    noFilter: No filter applied. To add one, use the form below the map
    save-confirmation: Do you want to export the map as PNG image or PDF ?
    noTemporalAreas: No temporal areas are displayed on the map.
    marketFrom: From
    marketTo: To
    dateRange: Handle scientific objects with date range
    export-error: The number of exported objects exceeds the limit of 10,000 objects.
    export-info: The export can take some time (about 1min for 7000 objects).
    export-no-found: No items to export.
    export: Export items
  Area:
    title: Area
    add: Description of the area
    update: Update Area
    display: Areas
  Filter:
    add: Creation of the filter
    update: Update filter
    created: the filter
  ScientificObjects:
    info: No geolocated scientific objects in this experiment.
    title: Scientific object
    update: Scientific object has been updated
    display: Scientific objects
  Device:
    title: Device
    update: Update device
    en:
  help: Instructions

fr:
  MapView:
    name: nom
    description: description
    type: type
    label: Géométrie
    add-button: Ajouter des métadonnées
    add-area-button: Ajouter une zone
    delete-button: Supprimer l'élément
    selected-button: Sortir du mode création
    errorLongitude: la longitude doit être comprise entre -180 et 180
    errorLatitude: la latitude doit être comprise entre -90 et 90
    Legend: Légende
    LegendSO: Objet scientifique
    LegendStructuralArea: Zone structurelle
    LegendTemporalArea: Zone temporaire
    Instruction: Appuyez sur Maj pour <b>sélectionner élément par élément</b> sur la carte.<br> Appuyez et maintenez Maj +Alt + Clic puis déplacer la souris pour faire <b>pivoter</b> la carte.<br> Appuyez sur Ctrl + Clic tout en faisant glisser pour <b>sélectionner plusieurs objets scientifiques</b>.<br> Appuyez sur Maj + Clic tout en faisant glisser pour définir une <b>zone de zoom avant</b>.<br> Pour afficher un graphique, sélectionner un à 15 objets scientifiques sur la carte et cliquer sur le bouton "Afficher le graphique".
    WarningInstruction: Actuellement, l'outil de sélection ne suit pas la rotation.
    details: Afficher ou masquer les détails de l'élément
    author: Auteur
    update: Mise à jour de l'élément
    display: Couches
    displayFilter: Filtres ({count})
    mapPanel: Gérer la carte
    mapPanelTitle: Données
    mapPanelNotVisible : non visible
    eventPanelTitle: Évènements
    eventPanel-help: Le panneau des événements affiche tous les événements liés aux zones temporaires. Ils sont affichés des plus récents aux plus anciens.
    mapPanelScientificObjects: Objets Scientifiques
    mapPanelAreas: Zones
    mapPanelAreasStructuralArea: Zone structurelle
    mapPanelAreasTemporalArea: Zone temporaire
    mapPanelFilters: Filtres
    mapPanelDevices : Appareils
    create-filter: Créer un filtre
    center: Recentrer la carte
    print: Imprimer la carte
    chart: Afficher le graphique - de 1 à 15 objets scientifiques
    time: Visualiser les zones temporaires
    noFilter: Aucun filtre appliqué. Pour en ajouter, utiliser le formulaire situé sous la carte
    save-confirmation: Voulez-vous exporter la carte au format PNG ou PDF ?
    noTemporalAreas: Aucune zone temporaire n'est affichée sur la carte.
    marketFrom: Du
    marketTo: Jusqu'au
    dateRange: Manipuler des objets scientifiques avec une plage de dates
    export-error: Le nombre d'objets exportés est supérieur à la limite de 10 000 objets.
    export-info: L'export peut prendre un certain temps (environ 1min pour 7000 objets).
    export-no-found: Aucun élément à exporter.
    export: Exporter les éléments
  Area:
    title: Zone
    add: Description de la zone
    update: Mise à jour de la zone
    display: Zones
  Filter:
    add: Création d'un filtre
    update: Mise à jour du filtre
    created: Le filtre
  ScientificObjects:
    info: Aucun objet scientifique géolocalisé dans cette expérimentation.
    title: Objet scientifique
    update: L'objet scientifique a été mis à jour
    display: Objets scientifiques
  Device:
    title: Dispositif
    update: Mise à jour du dispositif
  help: Instructions
</i18n>
