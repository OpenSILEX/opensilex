<template>
  <div id="map">
    <opensilex-Tutorial
      ref="mapTutorial"
      :steps="tutorialSteps"
      :editMode="false"
    ></opensilex-Tutorial>
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
    <!--Update SO form -->
    <opensilex-ScientificObjectForm
      v-if="user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
      ref="soForm"
      :context="{ experimentURI: this.experiment }"
      @onUpdate="callScientificObjectUpdate"
    />
    <!-- update device modal-->
    <opensilex-ModalForm
      ref="deviceForm"
      component="opensilex-DeviceForm"
      editTitle="Device.update"
      icon="fa#sun"
      modalSize="lg"
      @onUpdate="zoomRestriction"
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
        <div>
          <br />
          <b-alert v-model="showInstructionMap" dismissible>
            <p v-html="$t('MapView.Instruction')"></p>
          </b-alert>
        </div>
      </div>
    </div>
    <!----------------------------- Editing area mode ---------------------->
    <div v-if="editingMode" id="editing">
      <!--exit button -->
      <opensilex-Button
        :small="false"
        icon
        label="MapView.selected-button"
        variant="secondary"
        @click="editingMode = false"
      ></opensilex-Button>
    </div>

    <!------------------------------------- MAP -------------------------------->
    <!--If editing mode = true >grey frame -->
    <div
      id="mapPoster"
      :class="editingMode ? 'bg-light border border-secondary position-relative;' : ''"
    >
      <!-- Map config - "mapControls" to display the scale -->

      <vl-map
        ref="map"
        v-if="!comparationMode"
        :default-controls="mapControls"
        :load-tiles-while-animating="true"
        :load-tiles-while-interacting="true"
        class="map"
        data-projection="EPSG:4326"
        style="height: 80vh"
        @created="mapCreated"
        @moveend="zoomRestriction"
        @click="updateSelectionFeatures"
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
        <!-- stop-event property to be sure the map is not glitching vertically -->
        <vl-overlay
          id="overlay"
          :position="overlayCoordinate.length === 2 ? overlayCoordinate : [0, 0]"
          :stop-event="selectPointerMove.name && selectPointerMove.type ? true : false"
        >
          <template slot-scope="scope">
            <div class="panel-content">
              {{ displayInfoInOverlay() }}
            </div>
          </template>
        </vl-overlay>
        <!--Detail Pop-up -->

        <vl-overlay id="detailItem" :position="overlayCoordinate">
          <template v-slot="scope">
            <div class="panel-content"></div>
          </template>
        </vl-overlay>
        <!-- Vectors -->
        <template v-if="endReceipt">
          <!-- OS features featuresOS-->
          <vl-layer-vector :visible="checkZoom" render-mode="image" :z-index="1">
            <vl-source-cluster :distance="25">
              <vl-source-vector ref="clusterSourceSite"></vl-source-vector>
              <vl-style-func :factory="makeClusterStyleFunc"></vl-style-func>
            </vl-source-cluster>
          </vl-layer-vector>
          <vl-layer-vector :opacity="opacityOS" render-mode="image" :z-index="0">
            <vl-source-vector
              ref="vectorSourceSite"
              :features="featuresSites"
              @mounted="defineCenter"
            ></vl-source-vector>
            <vl-style-box>
              <vl-style-circle :radius="5">
                <vl-style-stroke color="blue"></vl-style-stroke>
                <vl-style-fill color="red"></vl-style-fill>
              </vl-style-circle>
            </vl-style-box>
          </vl-layer-vector>
          <vl-layer-vector
            :visible="checkZoom"
            render-mode="image"
            :z-index="1"
            :max-resolution="13"
          >
            <vl-source-cluster :distance="25">
              <vl-source-vector
                ref="clusterSource"
                :features="featuresSites"
              ></vl-source-vector>
              <vl-style-func :factory="makeClusterStyleFunc"></vl-style-func>
            </vl-source-cluster>
          </vl-layer-vector>
          <vl-layer-vector render-mode="image" :z-index="0" :min-resolution="35">
            <vl-source-vector
              ref="vectorSource"
              :features="featuresSites"
              @mounted="defineCenter"
            ></vl-source-vector>
            <vl-style-func :factory="createStyleFunction"></vl-style-func>
          </vl-layer-vector>
          <!-- Facilities -->
          <vl-layer-vector render-mode="image">
            <vl-source-vector :features="facilitiesData" :wrap-x="true">
              <vl-style-box>
                <vl-style-stroke color="red" :width="5"></vl-style-stroke>
                <vl-style-fill color="rgba(0,   255,   255,   1)"></vl-style-fill>
              </vl-style-box>
            </vl-source-vector>
          </vl-layer-vector>
          <!-- Filters -->
          <vl-layer-vector
            v-for="layer in tabLayer"
            :key="layer.ref"
            :visible="displayFilters && layer.display === 'true'"
          >
            <vl-source-vector
              :ref="layer.ref"
              :features.sync="layer.tabFeatures"
            ></vl-source-vector>
            <vl-style-box>
              <!-- outline color -->
              <vl-style-stroke
                v-if="layer.vlStyleStrokeColor"
                :color="layer.vlStyleStrokeColor"
              ></vl-style-stroke>
              <vl-style-fill
                v-if="layer.vlStyleFillColor"
                :color="colorFeature(layer.vlStyleFillColor)"
              ></vl-style-fill>
              <vl-style-circle :radius="5">
                <!-- outline color -->
                <vl-style-stroke
                  v-if="layer.vlStyleStrokeColor"
                  :color="layer.vlStyleStrokeColor"
                ></vl-style-stroke>
                <vl-style-fill
                  v-if="layer.vlStyleFillColor"
                  :color="colorFeature(layer.vlStyleFillColor)"
                ></vl-style-fill>
              </vl-style-circle>
            </vl-style-box>
          </vl-layer-vector>
        </template>

        <!-- Interaction for selecting vector features -->
        <vl-interaction-select
          v-if="!editingMode"
          id="select"
          :features="selectedFeatures"
          @update:features="updateSelectionFeatures"
        />
      </vl-map>
      <div v-if="comparationMode" class="mb-30">
        <b-row class="justify-content-center">
          <b-col cols="12">
            <div class="scroll-container">
              <div class="scroll-content">
                <div
                  v-for="(data, index) in documentFromSites"
                  :key="index"
                  class="scroll-item"
                >
                  <site-card
                    :data="data"
                    :filter="filter"
                    :index="index"
                    :year="selectedYear"
                  ></site-card>
                </div>
              </div>
            </div>
          </b-col>
        </b-row>
      </div>
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
          <div v-if="!comparationMode">
            <opensilex-InputForm
              label="Keywords"
              style="width: 80%; margin-left: 5% !important"
              type="text"
            ></opensilex-InputForm>
            <div class="mt-10 mb-10">
              <opensilex-FilterField>
                <opensilex-DateForm
                  :value.sync="filter.start_date"
                  label="component.common.begin"
                  name="startDate"
                  :max-date="filter.end_date ? filter.end_date : undefined"
                  class="custom-width"
                ></opensilex-DateForm>
              </opensilex-FilterField>
            </div>
            <div class="mb-10">
              <opensilex-FilterField>
                <opensilex-DateForm
                  :value.sync="filter.end_date"
                  label="component.common.end"
                  name="endDate"
                  :min-date="filter.start_date ? filter.start_date : undefined"
                  :minDate="filter.start_date"
                  :maxDate="filter.end_date"
                  class="custom-width"
                ></opensilex-DateForm>
              </opensilex-FilterField>
            </div>
            <opensilex-CreateButton
              v-if="!comparationMode"
              class="mt-15 ml-50 mb-20 mx-auto"
              label="Apply Filters"
              @click="compareSites(checkedSites, 0)"
            >
            </opensilex-CreateButton>
          </div>

          <div v-if="comparationMode">
            <select v-model="selectedYear">
              <option v-for="year in availableYears" :key="year" :value="year">
                {{ year }}
              </option>
            </select>
          </div>
          <template>
            <div class="align-items: center">
              <table class="table">
                <thead>
                  <tr>
                    <th>Sites</th>
                    <th>Select</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(site, index) in featuresSites" :key="index">
                    <td>{{ site.properties.name }}</td>
                    <td>
                      <input
                        type="checkbox"
                        :value="site.properties.uri"
                        v-model="selectedSites"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
              <opensilex-Button
                v-if="comparationMode"
                class="ml-50 mb-10 mx-auto"
                :label="`${refreshButtonText}`"
                @click="compareSites(checkedSites, 0)"
                icon="fa#refresh"
              >
              </opensilex-Button>
              <opensilex-CreateButton
                class="ml-50 mt-10"
                :label="`${compareButtonText}`"
                @click="compareSites(selectedSites, 1)"
              ></opensilex-CreateButton>
            </div>
          </template>
        </b-tabs>
      </template>
    </b-sidebar>

    <!--------------------- LEGEND ----------------------------->
    <div v-if="!comparationMode">
      {{ $t("MapView.Legend") }}:
      <span id="Sites" :style="{ color: 'orange' }">{{ $t("MapView.LegendSites") }}</span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { Circle as CircleStyle, Fill, Stroke, Style, Text } from "ol/style";
import { DragBox } from "ol/interaction";
import * as olExtent from "ol/extent";
import Point from "ol/geom/Point";
import { platformModifierKeyOnly } from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
import GeoJSONFeature from "vuelayers/src/ol-ext/format";
import {
  DocumentMetadataGetDTO,
  ExperimentsService,
  OrganizationsService,
  ScientificObjectsService,
  DocumentsService,
  ExperimentGetDTO,
  ScientificObjectDetailDTO,
  ScientificObjectNodeDTO,
  SiteGetListDTO,
} from "opensilex-core/index";
import Tutorial from "../../common/views/Tutorial.vue";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { transformExtent } from "vuelayers/src/ol-ext/proj";
import { defaults, ScaleLine } from "ol/control";
import { jsPDF } from "jspdf";
import { saveAs } from "file-saver";
import { Store } from "vuex";
import VueI18n from "vue-i18n";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import ExperimentDataVisualisation from "../experiments/ExperimentDataVisualisation.vue";
import SiteCard from "./SiteCard.vue";

@Component({
  components: {
    ExperimentDataVisualisation,
    SiteCard,
  },
})
export default class MapView extends Vue {
  @Ref("mapView") readonly mapView!: any;
  @Ref("map") readonly map!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("clusterSource") readonly clusterSource!: any;
  @Ref("filterForm") readonly filterForm!: any;
  @Ref("soForm") readonly soForm!: any;
  @Ref("deviceForm") readonly deviceForm!: any;
  @Ref("exportShapeModalList") readonly exportShapeModalList!: any;
  @Ref("mapSidebar") readonly mapSidebar!: any;
  @Ref("mapTutorial") readonly mapTutorial!: Tutorial;

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
  private experiment: string = "";
  private experimentService: ExperimentsService;
  private documentsService: DocumentsService;
  private scientificObjectsService: ScientificObjectsService;
  private organizationsService: OrganizationsService;
  private editingMode: boolean = false;

  ///////////// MAP PANEL ////////////
  isDisabled: boolean = true;
  private scientificObjects: any = [];
  private displaySO: boolean = true;
  private displayFilters: boolean = true;
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
  selectedSites: string[] = [];
  checkedSites: string[] = [];

  ///////////// FEATURES DATA ////////////
  private endReceipt: boolean = false;
  featuresOS: GeoJSONFeature[][] = [];
  featuresSites: GeoJSONFeature[] = [];
  private callSO: boolean = false;
  private callSites: boolean = false;
  private showNextLevel: boolean = false;
  private scientificObjectURI: string;

  checkZoom: boolean = true;
  opacityOS: number = 0;
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

  ///////////// TOOLS BUTTONS ////////////
  showInstructionMap: boolean = false;

  ///////////// EVENT PANEL ////////////
  timelineSidebarVisibility: boolean = false;

  ///////////// DATE RANGE ////////////
  filter = {
    start_date: undefined,
    end_date: undefined,
  };

  exportedOS = [];
  exportedDevices = [];
  exportedAreas = [];
  mapMode: boolean = true;
  soWithLabels: any[] = [];
  facilitiesData: GeoJSONFeature[] = [];
  documentFromSites: DocumentMetadataGetDTO[] = [];
  comparationMode: boolean = false;
  compareButtonText: String = "Compare sites";
  refreshButtonText: String = "Refresh";

  selectedYear: number = 2022;
  availableYears: number[] = [2021, 2022, 2023, 2024];
  update: boolean = false;

  ///////////// BASE METHODS ////////////
  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get isMapHasLayer() {
    return this.featuresOS.length > 0 || this.featuresSites.length > 0;
  }

  get tutorialSteps(): any[] {
    return [
      {
        target: "#map",
        header: { title: "TUTO 1 TITRE" },
        content: "TUTO TEST 1 ",
        params: { placement: "bottom" },
      },
      {
        target: "#mapPoster",
        header: { title: "TUTO 2 TITRE" },
        content: "TUTO TEST 2 ",
        params: { placement: "bottom" },
      },
      {
        target: "#mapPoster",
        header: { title: "TUTO 3 TITRE" },
        content: "TUTO TEST 3 ",
        params: { placement: "bottom" },
      },
    ];
  }

  triggerUpdate() {
    this.update = true;
    this.$nextTick(() => {
      this.update = false;
    });
  }

  created() {
    this.$opensilex.showLoader();

    this.scientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    this.documentsService = this.$opensilex.getService("opensilex.DocumentsService");
    this.experimentService = this.$opensilex.getService("opensilex.ExperimentsService");
    this.organizationsService = this.$opensilex.getService(
      "opensilex.OrganizationsService"
    );

    this.experiment = "dev:id/experiment/bourdic_xp";

    this.experimentService
      .getExperiment(this.experiment)
      .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
        this.experimentData = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);

    this.recoveryScientificObjects();
    this.recoverySites();
    this.soFilter = {
      name: "",
      experiment: this.experiment,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };
  }

  compareSites(sites, value) {
    this.documentFromSites = [];
    if (sites.length == 0) sites = this.selectedSites.slice();
    this.documentsService
      .getMetadataByTargetsAndDates(sites, this.filter.start_date, this.filter.end_date)
      .then((http: HttpResponse<OpenSilexResponse<Array<DocumentMetadataGetDTO>>>) => {
        const result = http.response.result;
        this.documentFromSites = result.slice();
      })
      .catch(this.$opensilex.errorHandler);
    if (value) {
      this.comparationMode = !this.comparationMode;
      if (this.comparationMode) this.compareButtonText = "Return to map";
      if (!this.comparationMode) this.compareButtonText = "Compare sites";
    }
  }

  createStyleFunction() {
    return function (feature) {
      // Create a style for each feature
      const style = new Style({
        image: new CircleStyle({
          radius: 5,
          stroke: new Stroke({
            color: "grey",
          }),
          fill: new Fill({
            color: "orange",
          }),
        }),
        text: new Text({
          text: feature.get("name") || "", // Access the 'name' property of the feature
          offsetY: -15,
          fill: new Fill({
            color: "black",
          }),
        }),
      });

      return style;
    };
  }

  removeFromFeatures(uri, features) {
    features.splice(
      features.findIndex((feature) => feature.properties.uri === uri),
      1
    );
    return features;
  }

  ///////////// MAP METHODS ////////////
  displayInfoInOverlay(): string {
    if (this.selectPointerMove.name && this.selectPointerMove.type) {
      return this.selectPointerMove.name + " (" + this.selectPointerMove.type + ")";
    } else {
      return "";
    }
  }

  private multiSelect(map) {
    const dragBox = new DragBox({
      condition: platformModifierKeyOnly,
      onBoxEnd: () => {
        if (this.isMapHasLayer) {
          const extent = dragBox.getGeometry().getExtent();
          const layers = this.map.$map.getLayers().getArray();
          for (let i = 0; i < layers.length; i++) {
            const layer = layers[i];
            if (layer.getVisible() && layer.type === "VECTOR") {
              const source = layer.getSource();
              const features = source.getFeaturesInExtent(extent);
              if (features.length > 0) {
                features.forEach((feature) => {
                  feature = olExt.writeGeoJsonFeature(feature);
                  this.selectedFeatures.push(feature);
                });
                break;
              }
            }
          }
        }
      },
    });

    map.addInteraction(dragBox);

    this.map.$map.on("click", (event) => {
      let isFeatureSelected = this.map.$map.getFeaturesAtPixel(event.pixel);
      if (!isFeatureSelected && !this.editingMode) {
        this.selectedFeatures = [];
        this.selectedOS = [];
        this.soWithLabels = [];
        this.selectedSites = [];
      }
    });

    dragBox.on("boxstart", () => {
      this.selectedFeatures = [];
      this.selectedOS = [];
      this.soWithLabels = [];
    });
  }

  mapCreated(map) {
    this.multiSelect(map);
  }

  defineCenter() {
    if (this.featuresSites.length > 0 && this.vectorSource) {
      this.mapView.$view.fit(this.vectorSource.$source.getExtent());
    }
  }

  select(value) {
    this.$emit("select", value);
  }

  selectSites(features) {
    this.selectedSites = [];
    features.forEach((feature) => {
      if (feature.properties) {
        this.selectedSites.push(feature.properties.uri);
      } else {
        this.selectedSites.push(feature.getProperties().uri);
      }
    });
    // this.showPopup = true;
  }

  updateSelectionFeatures(features) {
    if (features.length && features[0]) {
      this.selectedFeatures = features;
      if (features[0].properties.nature === "Sites") {
        this.selectSites(features);
      }
    } else {
      // this.showPopup = false;
    }

    return this.selectedFeatures.length === 1
      ? this.showDetails(this.selectedFeatures[0], true)
      : "";
  }

  private overlayPositionsRecovery() {
    let coordinateExtent = this.getCoordinateExtent();

    this.calcOverlayCoordinate(coordinateExtent);

    this.calcCenterMap(coordinateExtent);
  }

  private getCoordinateExtent() {
    return transformExtent(
      this.mapView.$view.calculateExtent(),
      "EPSG:3857",
      "EPSG:4326"
    );
  }

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

  private zoomRestriction() {
    if (this.mapView.$view.getZoom() < 5) {
      this.isDisabled = true;
      this.checkZoom = true;
      this.opacityOS = 0;
    } else {
      if (this.mapView.$view.getZoom() < 13) {
        this.showNextLevel = false;
        this.checkZoom = true;
        this.opacityOS = 0;
      } else {
        this.showNextLevel = true;
        this.checkZoom = false;
        this.opacityOS = 1;
      }
      this.isDisabled = false;
      let coordinateExtent = this.getCoordinateExtent();

      this.calcOverlayCoordinate(coordinateExtent);

      this.calcCenterMap(coordinateExtent);

      if (
        coordinateExtent[0] >= -180 &&
        coordinateExtent[0] <= 180 &&
        coordinateExtent[2] >= -180 &&
        coordinateExtent[2] <= 180 &&
        coordinateExtent[1] >= -90 &&
        coordinateExtent[1] <= 90 &&
        coordinateExtent[3] >= -90 &&
        coordinateExtent[3] <= 90
      ) {
        let geometry = {
          type: "Polygon",
          coordinates: [
            [
              [coordinateExtent[2], coordinateExtent[1]],
              [coordinateExtent[0], coordinateExtent[1]],
              [coordinateExtent[0], coordinateExtent[3]],
              [coordinateExtent[2], coordinateExtent[3]],
              [coordinateExtent[2], coordinateExtent[1]],
            ],
          ],
        };
      }
    }
  }

  showChart() {
    this.$bvModal.show("modal-chart");
    this.mapSidebar.hide();
  }

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
        this.vectorSource.length === this.featuresSites.length &&
        this.vectorSource.every(isVectorSourceMounted)
      ) {
        this.vectorSource.forEach((vector) => {
          if (vector.$parent.$layer.getVisible()) {
            features.push(vector.getFeatures());
          }
        });
        this.$opensilex.hideLoader();
        this.clusterSource.$source.addFeatures(features.flat());
      }
    }
  }

  manageCluster(e) {
    this.map.forEachFeatureAtPixel(e.pixel, (feature) => {
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
        this.mapView.$view.fit(olExtent.boundingExtent(points), { maxZoom: 14 });
      }
    });
  }

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
              color: "black",
            }),
            fill: new Fill({
              color: "#ADD8E6",
            }),
          }),
          text: new Text({
            text: size.toString(),
            fill: new Fill({
              color: "black",
            }),
          }),
        });
        styleCache[size] = style;
      }
      return style;
    };
  }

  showDetails(data, isMap = false) {
    if (isMap) {
      let uriResult = data.properties.uri;
      if (data.properties.nature === "ScientificObjects") {
        this.scientificObjectsDetails(uriResult);
      }
    } else {
      if (!data.detailsShowing) {
        let uriResult = data.item.properties.uri;
        if (data.item.properties.nature === "ScientificObjects") {
          this.scientificObjectsDetails(uriResult);
        }
      }
      data.toggleDetails = -data.toggleDetails();
    }
  }

  callScientificObjectUpdate() {
    if (this.callSO) {
      this.callSO = false;
      this.removeFromFeatures(this.scientificObjectURI, this.selectedFeatures);
      this.scientificObjectsService
        .getScientificObjectDetail(this.scientificObjectURI, this.experiment)
        .then((http: HttpResponse<OpenSilexResponse<ScientificObjectDetailDTO>>) => {
          const result = http.response.result as any;
          if (result.geometry !== null) {
            result.geometry.properties = {
              uri: result.uri,
              name: result.name,
              type: result.rdf_type,
              nature: "ScientificObjects",
              creation_date: result.creation_date,
              destruction_date: result.destruction_date,
              rdf_type_name: result.rdf_type_name,
              relations: result.relations,
            };
            let flatFeatures = this.featuresOS.flat();
            flatFeatures.splice(
              flatFeatures.findIndex(
                (feature) => feature.properties.uri === result.geometry.properties.uri
              ),
              1,
              result.geometry
            );
            this.featuresOS = [];
            flatFeatures.forEach((element) => {
              let inserted = false;
              this.featuresOS.forEach((item) => {
                if (item[0].properties.type === element.properties.type) {
                  item.push(element);
                  inserted = true;
                }
              });
              if (!inserted) {
                this.featuresOS.push([element]);
              }
            });
            this.selectedFeatures.push(result.geometry);
          }
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  private recoveryScientificObjects(startDate?, endDate?) {
    this.callSO = false;
    this.featuresOS = [];
    this.scientificObjectsService
      .searchScientificObjectsWithGeometryListByUris(this.experiment, startDate, endDate)
      .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
        const res = http.response.result as any;
        let documentIndex = 0;
        res.forEach((element) => {
          if (element.geometry !== null) {
            element.geometry.properties = {
              creation_date: element.creation_date,
              destruction_date: element.destruction_date,
              uri: element.uri,
              name: element.name,
              type: element.rdf_type,
              rdf_type_name: element.rdf_type_name,
              nature: "ScientificObjects",
            };
            let inserted = false;
            this.featuresOS.forEach((item) => {
              if (item[0].properties.type === element.rdf_type) {
                item.push(element.geometry);
                inserted = true;
              }
            });
            if (!inserted) {
              this.featuresOS.push([element.geometry]);
            }
            documentIndex++;
          }
        });
        if (res.length !== 0) {
          this.endReceipt = true;
        }
      })
      .catch((e) => {
        this.$opensilex.errorHandler(e);
        this.$opensilex.hideLoader();
      })
      .finally(() => {
        this.initScientificObjects();
      });
  }

  private recoverySites(startDate?, endDate?) {
    this.callSites = false;
    this.featuresSites = [];
    this.organizationsService
      .searchSitesWithGeometry()
      .then((http: HttpResponse<OpenSilexResponse<Array<SiteGetListDTO>>>) => {
        const res = http.response.result as any;
        res.forEach((element) => {
          if (element.geometry !== null) {
            element.geometry.properties = {
              creation_date: element.creation_date,
              destruction_date: element.destruction_date,
              uri: element.uri,
              name: element.name,
              type: element.rdf_type,
              rdf_type_name: element.rdf_type_name,
              nature: "Sites",
            };

            this.featuresSites.push(element.geometry);
          }
        });
        if (res.length !== 0) {
          this.endReceipt = true;
        }
      })
      .catch((e) => {
        this.$opensilex.errorHandler(e);
        this.$opensilex.hideLoader();
      });
  }

  private scientificObjectsDetails(scientificObjectUri: any) {
    if (scientificObjectUri != undefined) {
      this.$opensilex.disableLoader();
      this.scientificObjectsService
        .getScientificObjectDetail(scientificObjectUri, this.experiment)
        .then((http: HttpResponse<OpenSilexResponse<ScientificObjectDetailDTO>>) => {
          let result = http.response.result;

          this.selectedFeatures.forEach((item) => {
            if (item.properties.uri === result.uri) {
              item.properties.OS = result;
            }
          });
        })
        .catch(this.$opensilex.errorHandler)
        .finally(() => {
          this.$opensilex.enableLoader();
        });
    }
  }

  get titleFile(): String {
    return "map";
  }

  printMap() {
    this.$bvModal.show("modal-print-map");
  }

  savePNG(titleFile: String) {
    let canvas = document.getElementsByTagName("canvas")[0];
    canvas.toBlob((blob) => {
      saveAs(blob, titleFile + ".png");
    });
    this.$bvModal.hide("modal-print-map");
  }

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

  exportMap() {
    let exportedFeat = [];

    if (this.selectedFeatures.length > 0) {
      exportedFeat = this.selectedFeatures;
    } else {
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

    this.exportedOS = exportedFeatures.filter(
      (feature) => feature.nature === "ScientificObjects"
    );
    this.exportedDevices = exportedFeatures.filter(
      (feature) => feature.nature === "Device"
    );
    this.exportedAreas = exportedFeatures.filter((feature) => feature.nature === "Area");

    if (
      this.exportedOS.length > 10000 ||
      this.exportedDevices.length > 10000 ||
      this.exportedAreas.length > 10000
    ) {
      this.$opensilex.showErrorToast(this.$i18n.t("MapView.export-error").toString());
    } else {
      let disabled = { SO: false, devices: false, areas: false };

      if (this.exportedOS.length === 0) {
        disabled.SO = true;
      }
      if (this.exportedDevices.length === 0) {
        disabled.devices = true;
      }
      if (this.exportedAreas.length === 0) {
        disabled.areas = true;
      }

      this.exportShapeModalList.show(disabled);
    }
  }

  downloadFeatures(values) {
    this.$opensilex.showInfoToast(this.$i18n.t("MapView.export-info").toString());

    if (this.exportedOS.length > 0) {
      this.buildRequest(
        "/core/scientific_objects/export_geospatial",
        "export_scientific_objects" + "_" + values.format,
        {
          selected_props: values.SO.props,
          experiment: this.experiment,
          format: values.format,
        },
        this.exportedOS
      );
    }
    if (this.exportedDevices.length > 0) {
      this.buildRequest(
        "/core/devices/export_geospatial",
        "export_devices" + "_" + values.format,
        { selected_props: values.devices.props, format: values.format },
        this.exportedDevices
      );
    }
    if (this.exportedAreas.length > 0) {
      this.buildRequest(
        "/core/area/export_geospatial",
        "export_areas" + "_" + values.format,
        { selected_props: values.areas.props, format: values.format },
        this.exportedAreas
      );
    }
    if (
      this.exportedAreas.length === 0 &&
      this.exportedOS.length === 0 &&
      this.exportedDevices.length === 0
    ) {
      this.$opensilex.showErrorToast(this.$i18n.t("MapView.export-no-found").toString());
    }
  }

  buildRequest(path: string, title: string, queryParams: any, body: any) {
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

  initScientificObjects() {
    let scientificObject: any = {
      title: "Scientific Objects",
      isLeaf: false,
      children: [],
      isExpanded: true,
      isSelected: null,
      isDraggable: false,
      isSelectable: false,
      isCheckable: true,
    };

    this.featuresOS.forEach((os) => {
      let bool = true;
      for (let children of scientificObject.children) {
        if (children.title === os[0].properties.type) {
          bool = false;
        }
      }
      if (bool) {
        let children = {
          title: os[0].properties.type,
          children: [],
          isLeaf: true,
          isSelectable: false,
          isDraggable: false,
          isCheckable: true,
          isExpanded: false,
          isSelected: null,
        };
        scientificObject.children.push(children);
      }
    });
    this.scientificObjects = [];
    this.scientificObjects.push(scientificObject);
  }
}
</script>

<style lang="scss" scoped>
.scroll-container {
  overflow-x: visible;
  margin-left: 0px;
}

.custom-width {
  width: 20vh;
}

.scroll-content {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: flex-start;
}

.scroll-item {
  flex: 0 0 auto;
  margin: 0 10px;
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
  max-width: 100%;
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
  color ::v-deep .b-sidebar > .b-sidebar-body {
    background-color: white;
  }
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
    LegendSites: Sites
    LegendStructuralArea: Structural Area
    LegendTemporalArea: Temporal Area
    Instruction: Press Shift to <b>select item by item</b> on the map.<br> Press and hold Shift + Alt + Click and move the mouse <b>to rotate</b> the map.<br> Press Ctrl + Click while dragging to <b>select multiple sites</b>.<br>Press Shift + Click while dragging to define a <b>zoom-in area</b>.<br> To display a graph, select at least 2 sites on the map and click on the "Compare Sites" button.
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
    title: Scientific object
    update: Scientific object has been updated
    display: Scientific objects
  Device:
    title: Device
    update: Update device
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
    LegendSites: Sites
    LegendStructuralArea: Zone structurelle
    LegendTemporalArea: Zone temporaire
    Instruction: Appuyez sur Maj pour <b>sélectionner élément par élément</b> sur la carte.<br> Appuyez et maintenez Maj +Alt + Clic puis déplacer la souris pour faire <b>pivoter</b> la carte.<br> Appuyez sur Ctrl + Clic tout en faisant glisser pour <b>sélectionner plusieurs sites</b>.<br> Appuyez sur Maj + Clic tout en faisant glisser pour définir une <b>zone de zoom avant</b>.<br> Pour afficher un graphique, sélectionner au moins 2 sites sur la carte et cliquer sur le bouton "Afficher le graphique".
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
    title: Objet scientifique
    update: L'objet scientifique a été mis à jour
    display: Objets scientifiques
  Device:
    title: Dispositif
    update: Mise à jour du dispositif
  help: Instructions
</i18n>
