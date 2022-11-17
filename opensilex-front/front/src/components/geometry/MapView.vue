<template>
  <div id="map">
    <div v-if="!editingMode" id="selected" class="d-flex">
      <!--tools buttons -->
      <div class="mr-auto p-2">
        <!--create area button -->
        <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
          label="MapView.add-area-button"
          @click="editingMode = true"
        ></opensilex-CreateButton>
        <!-- map panel button (toggle side bar) -->
        <b-button v-b-toggle.map-sidebar>{{ $t("MapView.mapPanel") }}</b-button>
        <!-- focus button -->
        <opensilex-Button
          icon="fa#crosshairs"
          label="MapView.center"
          @click="defineCenter"
        ></opensilex-Button>
        <!-- save map button -->
        <opensilex-Button
          icon="fa#save"
          label="MapView.save"
          @click="saveMap"
        ></opensilex-Button>
        <!-- save map modal -->
        <b-modal id="modal-save-map">
          <template #default>
            <p>{{ $t("MapView.save-confirmation") }}</p>
          </template>
          <template #modal-footer>
            <b-button variant="danger" @click="savePDF(titleFile)"
              >PDF</b-button
            >
            <b-button variant="info" @click="savePNG(titleFile)">PNG</b-button>
            <b-button variant="success" @click="saveShapefile(titleFile)"
              >Shapefile</b-button
            >
          </template>
        </b-modal>
        <!-- events panel button (toggle side bar) -->
        <b-button
          v-on:click="showTemporalAreas"
          v-b-toggle.event-sidebar
          :title="$t('MapView.time')"
        >
          <slot name="icon">
            <opensilex-Icon :icon="'ik#ik-activity'" />
          </slot>
        </b-button>
        <!-- time line button -->
        <opensilex-Button
          icon="fa#stopwatch"
          label="MapView.dateRange"
          @click="handleDateRangeStatus"
        ></opensilex-Button>
      </div>
      <!-- Info message - in waiting for the bug' s correction . TODO: delete when it will be OK-->
      <span class="p-2">
        <label class="alert-warning">
          <img
            alt="Warning"
            src="../../../theme/phis/images/construction.png"
          />
          {{ $t("MapView.WarningInstruction") }}
        </label>
      </span>
    </div>
    <!--editing area mode -->
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
    <!--editing area modal -->
    <opensilex-ModalForm
      v-if="!errorGeometry"
      ref="areaForm"
      :successMessage="successMessageArea"
      component="opensilex-AreaForm"
      createTitle="Area.add"
      editTitle="Area.update"
      icon="fa#sun"
      modalSize="lg"
      @onCreate="areaRecovery"
      @onUpdate="areaRecovery"
      :initForm="initAreaForm"
    ></opensilex-ModalForm>
    <!--Update SO form. TODO: same method call onCreate and onUpdate??-->
    <opensilex-ScientificObjectForm
      v-if=" user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID)"
      ref="soForm"
      :context="{ experimentURI: this.experiment }"
      @onCreate="callScientificObjectUpdate"
      @onUpdate="callScientificObjectUpdate"
    />
    <!--Map -->
    <!--If editing mode =true >grey frame -->
    <div id="mapPoster" :class="editingMode ? 'bg-light border border-secondary' : ''" >
      <p class="alert-info">
        <span v-if="!editingMode" v-html="$t('MapView.Instruction')"></span>
      </p>
      <!-- Map config - "mapControls" to display the scale -->
      <vl-map
        ref="map"
        :default-controls="mapControls"
        :load-tiles-while-animating="true"
        :load-tiles-while-interacting="true"
        class="map"
        data-projection="EPSG:4326"
        style="height: 500px"
        @created="mapCreated"
        @pointermove="onMapPointerMove"
        @moveend="zoomRestriction"
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
        <!--Name OS Pop-up -->
        <!-- position ternary to show the overlay only if mouse on Object else default value -->
        <!-- stop-event property to be sure the map is not glitching vertically -->
        <vl-overlay
          id="overlay"
          :position="overlayCoordinate.length === 2 ? overlayCoordinate : [0, 0]"
          :stop-event="selectPointerMove.name && selectPointerMove.type ? true : false"
        >
          <template slot-scope="scope">
            <div class="panel-content">{{ displayInfoInOverlay() }}</div>
          </template>
        </vl-overlay>
        <!--Detail Pop-up -->
        <vl-overlay
          id="detailItem"
          :position="centerMap.length === 2 ? centerMap : [0, 0]"
        >
          <template v-slot="scope">
            <div class="panel-content">
              <opensilex-DisplayInformationAboutItem
                :details-s-o="detailsSO"
                :experiment="experiment"
                :item="selectedFeatures[0]"
                :showName="true"
                :withBasicProperties="true"
              />
            </div>
          </template>
        </vl-overlay>
        <!-- Vectors -->
        <template v-if="endReceipt">
          <!-- Temporal and structural Areas -->
          <vl-layer-vector
            v-for="area in featuresArea"
            :key="area.id"
            :visible="isAreaVisible(area)"
          >
            <!-- Structural areas -->
            <div v-if="area.properties.type != temporalAreaType">
              <vl-source-vector
                ref="vectorSourceArea"
                :features="[area]"
              ></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="green"></vl-style-stroke>
                <vl-style-fill color="rgba(200,255,200,0.4)"></vl-style-fill>
              </vl-style-box>
            </div>
            <!-- temporal areas -->
            <div v-if="area.properties.type === temporalAreaType &&!isSelectedArea(area)">
              <vl-source-vector
                ref="vectorSourceArea"
                :features="[area]"
              ></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="red"></vl-style-stroke>
                <vl-style-fill color="rgba(128,139,150,0.4)"></vl-style-fill>
              </vl-style-box>
            </div>
            <!-- temporal areas ?? -->
            <div v-if="area.properties.type === temporalAreaType && isSelectedArea(area)">
              <vl-source-vector
                ref="vectorSourceArea"
                :features="[area]"
              ></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="#33A0CC" :width="3"></vl-style-stroke>
                <vl-style-fill color="rgba(255,255,255,0.6)"></vl-style-fill>
              </vl-style-box>
            </div>
          </vl-layer-vector>
          <!-- OS features -->
          <vl-layer-vector v-for="layerSO in featuresOS" :key="layerSO.id">
            <vl-source-vector
              ref="vectorSource"
              :features="layerSO"
            ></vl-source-vector>
          </vl-layer-vector>
          <!-- Devices features -->
          <vl-layer-vector v-for="layerDevice in featuresDevice" :key="layerDevice.id">
            <vl-source-vector
                ref="vectorSource"
                :features="[layerDevice]"
            ></vl-source-vector>
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
        <!-- Editing areas -->
        <template v-on="editingMode">
          <div id="editionMode">
            <vl-layer-vector :visible="false">
              <vl-source-vector
                :features.sync="temporaryArea"
                ident="the-source"
                @update:features="memorizesArea"
              ></vl-source-vector>
            </vl-layer-vector>

            <!-- Creating a new area -->
            <vl-interaction-draw
              v-if="editingMode"
              source="the-source"
              type="Polygon"
              @drawend="showCreateForm()"
            >
              <!-- features style-->
              <vl-style-box>
                <vl-style-stroke color="blue"></vl-style-stroke>
                <vl-style-fill color="rgba(255,255,255,0.5)"></vl-style-fill>
              </vl-style-box>
            </vl-interaction-draw>
          </div>
        </template>

        <!-- Interaction for selecting vector features -->
        <vl-interaction-select
          v-if="!editingMode"
          id="select"
          ref="selectInteraction"
          :features="selectedFeatures"
          @update:features="updateSelectionFeatures"
        />
    </vl-map>
    </div>
    <!-- event sidebar (toggle) -->
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
          class="
            b-sidebar-header
            header-brand
            hamburger-container
            opensilex-sidebar-header
          "
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
            <button
              class="hamburger hamburger--collapse is-active p-3"
              @click="hide"
            >
              <span class="hamburger-box">
                <span class="hamburger-inner"></span>
              </span>
            </button>
          </div>
        </div>
        <div v-if="temporalAreas.length === 0" class="p-3">
          <p>{{ $t("MapView.noTemporalAreas") }}</p>
        </div>
        <opensilex-Timeline
          :items="temporalAreas"
          :selectedFeatures="selectedFeatures"
          @onClick="selectFeaturesFromTimeline"
        >
        </opensilex-Timeline>
      </template>
    </b-sidebar>
    <!-- Side Manage Menu -->
    <b-sidebar id="map-sidebar" visible no-header class="sidebar-content">
      <template #default="{ hide }">
        <div
          class="
            b-sidebar-header
            header-brand
            hamburger-container
            opensilex-sidebar-header
          "
        >
          <div class="d-flex">
            <span id="map-sidebar___title__" class="text mr-auto p-3">{{
              $t("MapView.mapPanelTitle").toUpperCase()
            }}</span>
            <button
              class="hamburger hamburger--collapse is-active p-3"
              @click="hide"
            >
              <span class="hamburger-box">
                <span class="hamburger-inner"></span>
              </span>
            </button>
          </div>
        </div>
        <b-tabs content-class="mt-3">
          <!-- SO -->
          <opensilex-TreeView :nodes.sync="scientificObjects">
            <template v-slot:node="{ node }">
              <span class="item-icon"> </span>&nbsp;
              <span v-if="node.title === 'Scientific Object'"
                >{{ $t("MapView.mapPanelScientificObjects") }} ({{
                  getNumberScientificObjects(featuresOS)
                }})</span
              >
              <span v-else
                >{{ getType(node.data.properties.type) }} ({{
                  getNumberScientificObjectsByType(
                    getType(node.data.properties.type)
                  )
                }})</span
              >
            </template>

            <template v-slot:buttons="{ node }">
              <opensilex-CheckboxForm
                :value.sync="displaySO"
                class="col-lg-2"
                v-if="node.title === 'Scientific Object'"
                :small="true"
                @update:value="updateScientificObject(node)"
              ></opensilex-CheckboxForm>

              <opensilex-CheckboxForm
                :value="node.data.properties.display"
                :disabled="!displaySO"
                v-if="node.title != 'Scientific Object'"
                class="col-lg-2"
                :small="true"
                @update:value="updateScientificObject(node)"
              ></opensilex-CheckboxForm>
            </template>
          </opensilex-TreeView>
          <!-- areas -->
          <opensilex-TreeView :nodes.sync="areas" :class="isDisabled ? 'disabled' : ''">
            <template v-slot:node="{ node }" >
              <span class="item-icon"> </span>
              <span v-if="node.title === 'Areas'">{{ $t("MapView.mapPanelAreas") }} ({{ featuresArea.length }})</span>
              <span v-else >{{ $t("MapView.mapPanelAreas" + node.title) }} ({{ getNumberByArea(node.title) }})</span>
            </template>

            <template v-slot:buttons="{ node }" v-if="!isDisabled">
              <opensilex-CheckboxForm
                v-if="node.title === 'Areas'"
                :value.sync="displayAreas"
                class="col-lg-2"
                :small="true"
                @update:value="updateArea(node)"
              ></opensilex-CheckboxForm>
              <opensilex-CheckboxForm
                :disabled="!displayAreas"
                v-if="node.title === 'StructuralArea'"
                :value.sync="displayStructuralAreas"
                class="col-lg-2"
                @update:value="updateArea(node)"
                :small="true"
              ></opensilex-CheckboxForm>
              <opensilex-CheckboxForm
                :disabled="!displayAreas"
                v-if="node.title === 'TemporalArea'"
                :value.sync="displayTemporalAreas"
                class="col-lg-2"
                @update:value="updateArea(node)"
                :small="true"
              ></opensilex-CheckboxForm>
            </template>
          </opensilex-TreeView>
          <!-- Devices -->
          <opensilex-TreeView :nodes.sync="devices" :class="isDisabled ? 'disabled' : ''">
            <template v-slot:node="{ node }">
              <span class="item-icon"> </span>&nbsp;
              <span v-if="node.title === 'Devices'"
              >{{ $t("MapView.mapPanelDevices") }} ({{
                  getNumberDevices()
                }})</span
              >
            </template>

            <template v-slot:buttons="{ node }" v-if="!isDisabled">
              <opensilex-CheckboxForm
                  :value.sync="displayDevices"
                  class="col-lg-2"
                  v-if="node.title == 'Devices'"
                  :small="true"
                  @update:value="updateDevice(node)"
              ></opensilex-CheckboxForm>
            </template>
          </opensilex-TreeView>
          <!-- filters -->
          <opensilex-TreeView :nodes.sync="filters">
            <template v-slot:node="{ node }">
              <span class="item-icon"> </span>&nbsp;
              <span v-if="node.title ==='Filters'"
                >{{ $t("MapView.mapPanelFilters") }} ({{
                  tabLayer.length
                }})</span
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
              <div
                class="d-flex flex-row mx-auto"
                v-if="node.title != 'Filters'"
              >
                <opensilex-CheckboxForm
                  v-if="node.title != 'Filters'"
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
    <!-- Legend -->
    {{ $t("MapView.Legend") }}:
    <span id="OS">{{ $t("MapView.LegendSO") }}</span>
    &nbsp;-&nbsp;
    <span id="StructuralArea">{{ $t("MapView.LegendStructuralArea") }}</span>
    &nbsp;-&nbsp;
    <span id="TemporalArea">{{ $t("MapView.LegendTemporalArea") }}</span>
    <!-- TimeLine -->
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
    <!-- Features table -->
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
            :to="
              data.item.properties.nature === 'Area'
                ? {
                    path:
                      '/area/details/' +
                      encodeURIComponent(data.item.properties.uri),
                  }
                : {
                    path:
                      '/scientific-objects/details/' +
                      encodeURIComponent(data.item.properties.uri),
                  }
            "
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
            :details-s-o="detailsSO"
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
              v-if="
                data.item.properties.nature === 'Area'
                  ? user.hasCredential(
                      credentials.CREDENTIAL_AREA_MODIFICATION_ID
                    )
                  : user.hasCredential(
                      credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                    )
              "
              :small="true"
              label="MapView.update"
              @click="edit(data)"
            ></opensilex-EditButton>

            <opensilex-DeleteButton
              v-if="
                data.item.properties.nature === 'Area'
                  ? user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)
                  : user.hasCredential(
                      credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                    )
              "
              label="MapView.delete-button"
              @click="
                selectedFeatures.splice(
                  selectedFeatures.indexOf(data.item),
                  1
                ) && deleteItem(data)
              "
            ></opensilex-DeleteButton>
          </b-button-group>
        </template>
      </opensilex-TableView>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { DragBox } from "ol/interaction";
import { GeoJSON } from "ol/format";
import { Vector } from "ol/source";
import Feature from "ol/Feature";
import * as olExtent from "ol/extent";
import Polygon from "ol/geom/Polygon";
import Point from "ol/geom/Point";
import { platformModifierKeyOnly } from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
let shpwrite = require("shp-write");
import {AreaGetDTO, PositionsService, AreaService, EventGetDTO, ResourceTreeDTO, ScientificObjectDetailDTO, ScientificObjectNodeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { transformExtent } from "vuelayers/src/ol-ext/proj";
import { defaults, ScaleLine } from "ol/control";
import Oeso from "../../ontologies/Oeso";
import * as turf from "@turf/turf";
import { jsPDF } from "jspdf";
import { saveAs } from "file-saver";
import {TargetPositionCreationDTO} from "opensilex-core/model/targetPositionCreationDTO";
import {ExperimentGetDTO} from "opensilex-core/model/experimentGetDTO";

@Component
export default class MapView extends Vue {
  @Ref("JqxRangeSelector") readonly rangeSelector: any;
  @Ref("mapView") readonly mapView!: any;
  @Ref("map") readonly map!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("areaForm") readonly areaForm!: any;
  @Ref("filterForm") readonly filterForm!: any;
  @Ref("soForm") readonly soForm!: any;

  $opensilex: any;
  $t: any;
  $store: any;
  $i18n: any;
  $bvModal: any;
  el: "map";
  isDisabled: boolean = true;
  service: any;
  featuresOS: any[] = [];
  featuresArea: any[] = [];
  featuresDevice: Feature[] = [];
  temporaryArea: any[] = [];
  temporalAreas: any[] = [];
  selectPointerMove: { name: string; type: string } = {
    name: null,
    type: null,
  };
  overlayCoordinate: any[] = [];
  centerMap: any[] = [];
  tabLayer: any[] = [];
  selectInteraction: any = null;
  collection: any = null;
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
  selectedFeatures: any[] = [];
  timelineSidebarVisibility: boolean = false;
  minDate: Date = null;
  maxDate: Date = null;
  majorTicksInterval = "day";
  labelsFormat = "yyyy";
  minorTicksInterval = "hour";
  range: { from: Date; to: Date } = { from: null, to: null };
  // filter: any = {};
  experimentData: ExperimentGetDTO;

  private showArea: boolean = false;
  private editingMode: boolean = false;
  private displayDateRange: boolean = false;
  private displayAreas: boolean = true;
  private displayStructuralAreas: boolean = false;
  private displayTemporalAreas: boolean = false;
  private displayFilters: boolean = true;
  private displayDevices: boolean = true;
  private temporalAreaType: String = "vocabulary:TemporalArea";
  private structuralAreaType: String = "vocabulary:Area";
  private displaySO: boolean = true;
  private subDisplaySO: string[] = [];
  private detailsSO: boolean = false;
  private endReceipt: boolean = false;
  private errorGeometry: boolean = false;
  private callSO: boolean = false;
  private typeLabel: { uri: string; name: string }[] = [];
  private lang: string;
  private mapControls = defaults().extend([new ScaleLine()]);
  private experiment: string;
  private scientificObjectsService = "opensilex.ScientificObjectsService";
  private areaService: AreaService;
  private positionService: PositionsService;
  private scientificObjectURI: string;
  private scientificObjects: any = [];
  private devices: any = [{
    title: "Devices",
    isLeaf: false,
    children: [],
    isExpanded: true,
    isSelected: null,
    isDraggable: false,
    isSelectable: false,
    isCheckable: true,
  }];
  private areas: any = [
    {
      title: "Areas",
      isLeaf: false,
      children: [
        {
          title: "StructuralArea",
          isLeaf: true,
          isSelectable: false,
          isDraggable: false,
          isCheckable: true,
          isExpanded: false,
          isSelected: null,
          children: [],
        },
        {
          title: "TemporalArea",
          isLeaf: true,
          isSelectable: false,
          isDraggable: false,
          isCheckable: true,
          isExpanded: false,
          isSelected: null,
          children: [],
        },
      ],
      isExpanded: true,
      isSelected: null,
      isDraggable: false,
      isSelectable: false,
      isCheckable: true,
    },
  ];
  private langUnwatcher;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get isMapHasLayer() {
    return this.featuresOS.length > 0 || this.featuresArea.length > 0;
  }

  get titleFile(): String {
    return this.getType(this.$attrs.uri) + "-map";
  }

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
//initialize form area
  initAreaForm(form) {
    form.minDate = this.minDate;
    form.maxDate = this.maxDate;
    return form;
  }

  beforeDestroy() {
    this.langUnwatcher();
  }
//show Name pop-up
  displayInfoInOverlay(): string {
    if (this.selectPointerMove.name && this.selectPointerMove.type) {
      // if Object existing and filled
      return (
        this.selectPointerMove.name + " (" + this.selectPointerMove.type + ")"
      );
    } else {
      return "";
    }
  }

  getNumberScientificObjects(featureOs): Number {
    let res = 0;

    for (let layer of featureOs) {
      res += layer.length;
    }
    return res;
  }

  getNumberScientificObjectsByType(type: String): Number {
    let res = 0;

    for (let layer of this.featuresOS) {
      for (let obj of layer) {
        if (this.getType(obj.properties.type) === type) {
          res += 1;
        }
      }
    }
    return res;
  }

  getNumberByArea(areaType: String): Number {
    if (areaType === "StructuralArea") {
      return this.getNumberStructuralArea();
    } else {
      return this.getNumberTemporalArea();
    }
  }

  getNumberStructuralArea(): Number {
    let res = 0;

    for (let area of this.featuresArea) {
      if (area.properties.type != this.temporalAreaType) {
        res += 1;
      }
    }
    return res;
  }

  getNumberTemporalArea(): Number {
    let res = 0;

    for (let area of this.featuresArea) {
      if (area.properties.type === this.temporalAreaType) {
        res += 1;
      }
    }
    return res;
  }

  getNumberDevices(): Number {
    let res = 0;

    for (let layer of this.featuresDevice) {
      res += 1;
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
  updateDevice(node) {
    // Update the visibility of devices
    const type: "Devices" = node.title; // What node is clicked ?
    const layers = this.map.$map.getLayers();
    layers.forEach((element, index) => {
      // Iterate all layers of the map (OpenLayers API) to find the ones that are linked to Devices
      const source = element.getSource();
      if (source) {
        const collection = source.featuresCollection_;
        if (collection) {
          const array = collection.array_; // Get array of Features
          if (array && array.length > 0) {
            // We can check only the first element of the array
            // instead of check all elements (optimisation)
           if (
                array[0].values_.nature === "Device" && // Element must be of type 'Device'
                type === "Devices"
            ) {
              let status: boolean = false;
              if (type === "Devices") {
                status = this.displayDevices;
                if (status) {
                  // Trick to save or get the last visible status
                  status = element.get("last-visible-status");
                } else {
                  element.set("last-visible-status", element.getVisible());
                }
              } else {
                status = !element.getVisible(); // Inverse visibility status
              }
              if (element.getVisible() != status) {
                element.setVisible(status);
              }
            }
          }
        }
      }
    });
  }

  updateArea(node) {
    // Update the visibility of areas
    const type: "Areas" | "StructuralArea" | "TemporalArea" = node.title; // What node is clicked ?
    const layers = this.map.$map.getLayers();
    layers.forEach((element, index) => {
      // Iterate all layers of the map (OpenLayers API) to find the ones that are linked to ScientificObjects
      const source = element.getSource();
      if (source) {
        const collection = source.featuresCollection_;
        if (collection) {
          const array = collection.array_; // Get array of Features
          if (array && array.length > 0) {
            // We can check only the first element of the array
            // instead of check all elements (optimisation)
            if (
              array[0].values_.nature === "Area" && // Element must be of type 'Area'
              (type === "Areas" || // If 'Areas' node is clicked
                (type === "StructuralArea" &&
                  this.getType(array[0].values_.type) != "TemporalArea") || // If 'StructurArea' node is clicked and element is not temporal area type
                (type === "TemporalArea" &&
                  this.getType(array[0].values_.type) === "TemporalArea"))
            ) {
              // If 'TemporalArea' node is clicked and element is temporal area type
              let status: boolean = false;
              if (type === "Areas") {
                status = this.displayAreas;
                if (status) {
                  // Trick to save or get the last visible status
                  status = element.get("last-visible-status");
                } else {
                  element.set("last-visible-status", element.getVisible());
                }
              } else {
                status = !element.getVisible(); // Inverse visibility status
              }
              if (element.getVisible() != status) {
                element.setVisible(status);
              }
            }
          }
        }
      }
    });
  }
//Control show/hide SO on map panel
  updateScientificObject(node) {
    // Update the Scientific Objects of areas
    const isAllScientificObjects: boolean = node.title === "Scientific Object"; // is the 'Scientific Object' node clicked ?
    const layers = this.map.$map.getLayers();
    layers.forEach((element, index) => {
      // Iterate all layers of the map (OpenLayers API) to find the ones that are linked to ScientificObjects
      const source = element.getSource();
      if (source) {
        const collection = source.featuresCollection_;
        if (collection) {
          const array = collection.array_; // Get array of Features
          if (array && array.length > 0) {
            // We can check only the first element of the array
            // instead of check all elements (optimisation)
            if (
              array[0].values_.nature === "ScientificObjects" &&
              (isAllScientificObjects || // All Scientific Objects
                array[0].values_.name === node.title)
            ) {
              // Only the only type selected
              let status: boolean = false;
              if (isAllScientificObjects) {
                status = this.displaySO;
                if (status) {
                  // Trick to save or get the last visible status
                  status = element.get("last-visible-status");
                } else {
                  element.set("last-visible-status", element.getVisible());
                }
              } else {
                status = !element.getVisible(); // Inverse visibility status
              }
              if (element.getVisible() != status) {
                element.setVisible(status);
              }
            }
          }
        }
      }
    });
  }

  getType(type: string): String {
    const res = new URL(type);
    if (res.hash != "") {
      // long type (example: http://opensilex.dev/set/area#youpi)
      return res.hash;
    } else {
      // short type (example: Vocabulary#Area)
      return res.pathname;
    }
  }
//Show areas (Structural / temporal)
  isAreaVisible(area) {
      if (area.properties.type != this.temporalAreaType) {
        return this.displayStructuralAreas;
      } else if (area.properties.type != this.structuralAreaType) {
        return this.displayTemporalAreas;
      }
  }

  initScientificObjects() {
    let scientificObject: any = {
      title: "Scientific Object",
      data: {
        isCheckable: true,
        name: "Scientific Object",
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

    this.featuresOS.forEach((item) => {
      if (item[0].properties.name) {
        item[0].title = item[0].properties.name;
        item[0].isLeaf = true;
        item[0].isSelectable = false;
        item[0].isDraggable = false;
        item[0].isCheckable = true;
        item[0].isExpanded = false;
        item[0].isSelected = null;
        item[0].data = {
          children: [],
          name: item[0].properties.name,
          disabled: false,
          selected: false,
        };
        item[0].data.properties = item[0].properties;
        item[0].data.geometry = item[0].geometry;
      }
      scientificObject.children.push(item[0]);
    });
    this.scientificObjects = [];
    this.scientificObjects.push(scientificObject);
  }

  displayScientificObjects() {
    if (!this.displaySO) {
      for (const feature of this.featuresOS) {
        if (feature[0].properties.display === "true") {
          feature[0].properties.display = "false";
          this.subDisplaySO.push(feature[0].properties.uri);
        }
      }
    } else {
      for (const feature of this.featuresOS) {
        for (const uri of this.subDisplaySO) {
          if (feature[0].properties.uri === uri) {
            feature[0].properties.display = "true";
            this.subDisplaySO.splice(this.subDisplaySO.indexOf(uri), 1);
          }
        }
      }
    }
  }
//???
  isSelectedArea(area) {
    for (let selected of this.selectedFeatures) {
      if (selected.properties.uri === area.properties.uri) {
        return true;
      }
    }
    return false;
  }

  // Used to display details on the map and in the table
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

  showFiltersDetails(filterResult: any) {
    if (filterResult.ref) {
      this.tabLayer.forEach((element, index) => {
        if (element.ref === filterResult.ref) {
          this.tabLayer.splice(index, 1);
        }
      });
      this.tabLayer.push(filterResult);
    }
  }
  // Gets the name when the cursor hovers over the item.
  onMapPointerMove({ pixel }: any) {
    const hitFeature = this.map.forEachFeatureAtPixel(
      pixel,
      (feature) => feature
    );
    if (hitFeature) {
      this.selectPointerMove = {
        name: hitFeature.values_.name,
        type: this.nameType(hitFeature.values_.type),
      };
    } else {
      this.selectPointerMove = { name: null, type: null };
    }
  }

  onChangeDateRange(event) {
    //this.$opensilex.showLoader();
    this.range = { from: event.args.from, to: event.args.to };
    let startDate: string = this.formatDate(this.range.from);
    let endDate: string = this.formatDate(this.range.to);

    // this.filter.from =  startDate;
    // this.filter.to =  endDate;
    // this.$opensilex.updateURLParameters(this.filter);

    this.recoveryScientificObjects(startDate, endDate);
    this.zoomRestriction();
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
//select Features from timeline??
  selectFeaturesFromTimeline(uri) {
    for (let area of this.featuresArea) {
      if (area.properties.uri === uri) {
        for (let i = 0; i < this.selectedFeatures.length; i++) {
          if (this.selectedFeatures[i].properties.uri === uri) {
            this.selectedFeatures = [];
            return;
          }
        }
        this.selectedFeatures = [area];
        return;
      }
    }
  }
//update SO
  callScientificObjectUpdate() {
    if (this.callSO) {
      this.callSO = false;
      this.removeFromFeatureOS(this.scientificObjectURI, this.featuresOS);
      this.removeFromFeatureOS(this.scientificObjectURI, this.selectedFeatures);

      this.$opensilex
        .getService(this.scientificObjectsService)
        .getScientificObjectDetail(this.scientificObjectURI, this.experiment)
        .then(
          (
            http: HttpResponse<OpenSilexResponse<ScientificObjectDetailDTO>>
          ) => {
            const result = http.response.result as any;

            if (result.geometry != null) {
              result.geometry.properties = {
                uri: result.uri,
                name: result.name,
                type: result.rdf_type,
                nature: "ScientificObjects",
                display: "true",
              };

              let inserted = false;
              this.featuresOS.forEach((item) => {
                if (item[0].properties.type === result.rdf_type) {
                  item.push(result.geometry);
                  inserted = true;
                }
              });
              if (!inserted) {
                this.featuresOS.push([result.geometry]);
              }

              this.selectedFeatures.push(result.geometry);
            }
          }
        )
        .catch(this.$opensilex.errorHandler);
    }
  }
//Save created areas
  memorizesArea() {
    if (this.temporaryArea.length) {
      // Transfers geometry to the form using the $store
      this.$store.state.zone = this.temporaryArea.pop();

      let areaFeature = this.$store.state.zone;

      //To convert Polygon in MultiPolygon in store
      if (areaFeature.geometry.type === "Polygon") {
        let kinkedPoly = turf.polygon(areaFeature.geometry.coordinates);
        let unKinkedPoly = turf.unkinkPolygon(kinkedPoly);

        if (unKinkedPoly.features.length > 1) {
          let coordinates = [];

          unKinkedPoly.features.forEach((item) => {
            coordinates.push(item.geometry.coordinates);
          });

          this.$store.state.zone.geometry.type = "MultiPolygon";
          this.$store.state.zone.geometry.coordinates = coordinates;
        }
      }
      //Check coordinates in WGS84 CRS
      for (let element of areaFeature.geometry.coordinates[0]) {
        if (element[0] < -180 || element[0] > 180) {
          this.errorGeometry = true;
          alert(this.$i18n.t("MapView.errorLongitude"));
          return;
        }
        if (element[1] < -90 || element[1] > 90) {
          this.errorGeometry = true;
          alert(this.$i18n.t("MapView.errorLatitude"));
          return;
        }
        this.errorGeometry = false;
      }
    }
  }
//show created area form
  showCreateForm() {
    this.showArea = true;
    this.$nextTick(() => {
      this.areaForm.showCreateForm();
    });
  }
//show events panel
  showTemporalAreas() {
    if (
      this.displayAreas &&
      !this.displayTemporalAreas &&
      !this.timelineSidebarVisibility
    ) {
      // show temporal areas if clicking events panel
      this.displayTemporalAreas = true;
      this.updateArea({ title: "TemporalArea" });
    }
  }

  created() {
    this.$opensilex.showLoader();
    // this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
    this.experiment = decodeURIComponent(this.$route.params.uri);
    this.$opensilex
        .getService("opensilex.ExperimentsService")
        .getExperiment(this.experiment)
        .then((http) => {
          this.experimentData = http.response.result;
        })
        .catch(this.$opensilex.errorHandler);

    this.areaService = this.$opensilex.getService("opensilex.AreaService");
    this.positionService = this.$opensilex.getService("opensilex.PositionsService");

    this.retrievesNameOfType();
    this.recoveryScientificObjects();
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

    if (position === "left") res += this.$i18n.t("MapView.marketFrom");
    else res += this.$i18n.t("MapView.marketTo");
    res += ": ";
    if (this.$store.getters.language === "en") {
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
//Show time-line
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
      this.$opensilex
        .getService("opensilex.ExperimentsService")
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

// Select multi-features (OS, Areas and Filters)
  private multiSelect(map){
    // a DragBox interaction used to select features by drawing boxes
    const dragBox = new DragBox({
      condition: platformModifierKeyOnly,
      onBoxEnd: () => {
        // features that intersect the box are selected
        if (this.isMapHasLayer) {
          const extent = dragBox.getGeometry().getExtent();
          //All layers (tile/vector)
          var layers= this.map.$map.getLayers();
          //Recover vector and visible layers
          layers.forEach((layer)=>{
            if(layer.getVisible() && layer.type==='VECTOR'){
              const source = layer.getSource();
              source.forEachFeatureIntersectingExtent(
                  extent,
                  (feature: any) => {
                    feature = olExt.writeGeoJsonFeature(feature);
                    this.selectedFeatures.push(feature);
                  }
              )
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
      }
    });

    // clear selection when drawing a new box and when clicking on the map
    dragBox.on("boxstart", () => {
      this.selectedFeatures = [];
    });
  }

  mapCreated(map){
    this.multiSelect(map);
  }

  successMessageArea() {
    return this.$i18n.t("Area.title");
  }

  private waitFor(conditionFunction) {
    const poll = (resolve) => {
      if (conditionFunction()) resolve();
      else
        setTimeout((_) => {
          this.$opensilex.showLoader();
          poll(resolve);
        }, 200);
    };

    return new Promise(poll);
  }
//On click, focus on map vectors
  defineCenter(): Promise<boolean> {
    return new Promise((resolve, reject) => {
      if (this.featuresOS.length > 0) {
        try {
          const isVectorSourceMounted = (vector) =>
            vector &&
            vector.$source &&
            vector.$source.getExtent() &&
            vector.$source.getExtent()[0] &&
            vector.$source.getExtent()[0] != Infinity; // Condition to be sure sources has been mounted
          this.waitFor(
            (_) =>
              this.vectorSource.length === this.featuresOS.length &&
              this.vectorSource.every(isVectorSourceMounted)
          ) // Wait all vectors charged
            .then(() => {
              let extent = olExtent.createEmpty();
              for (let vector of this.vectorSource) {
                if (vector && vector.$source) {
                  let extentTemporary = vector.$source.getExtent();
                  olExtent.extend(extent, extentTemporary);
                }
              }
              this.mapView.$view.fit(extent);
              this.$opensilex.hideLoader();
              resolve(true);
            });
        } catch (e) {
          this.$opensilex.hideLoader();
          reject(false);
        }
      } else {
        resolve(false);
      }
    });
  }

  select(value) {
    this.$emit("select", value);
  }

  retrievesNameOfType() {
    let typeLabel: { uri: string; name: string }[] = [];
    this.lang = this.user.locale;

    this.service = this.$opensilex.getService("opensilex.OntologyService");

    this.service
      .getSubClassesOf(Oeso.SCIENTIFIC_OBJECT_TYPE_URI, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        const res = http.response.result;
        this.extracted(res, typeLabel);
      })
      .catch(this.$opensilex.errorHandler);

    this.service
      .getSubClassesOf(Oeso.AREA_TYPE_URI, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        const res = http.response.result;
        this.extracted(res, typeLabel);
      })
      .catch(this.$opensilex.errorHandler);

    this.typeLabel = typeLabel;
  }

  nameType(uriType): string {
    if (this.user.locale != this.lang) {
      this.retrievesNameOfType();
    }
    for (let typeLabelElement of this.typeLabel) {
      if (typeLabelElement.uri === uriType) return typeLabelElement.name;
    }
  }

  deleteItem(data) {
    let uri = data.item.properties.uri;

    if (data.item.properties.nature === "Area") {
      this.areaService
        .deleteArea(uri)
        .then((http) => {
              let message =
                  this.$i18n.t("Area.title") +
                  " " +
                  http.response.result +
                  " " +
                  this.$i18n.t("component.common.success.delete-success-message");
              this.$opensilex.showSuccessToast(message);
              this.removeFromFeaturesArea(uri, this.featuresArea);
            })
        .catch(this.$opensilex.errorHandler);
    } else {
      this.$opensilex
        .getService(this.scientificObjectsService)
        .deleteScientificObject(uri, this.experiment)
        .then((http) => {
          let message =
            this.$i18n.t("ScientificObjects.title") +
            " " +
            http.response.result +
            " " +
            this.$i18n.t("component.common.success.delete-success-message");
          this.$opensilex.showSuccessToast(message);
          this.removeFromFeatureOS(uri, this.featuresOS);
        })
        .catch(this.$opensilex.errorHandler);
    }
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
//On click, open modal "Save map"
  saveMap() {
    this.$bvModal.show("modal-save-map");
  }
//save and export the map in PNG format
  savePNG(titleFile: String) {
    let canvas = document.getElementsByTagName("canvas")[0];
    canvas.toBlob((blob) => {
      saveAs(blob, titleFile + ".png");
    });
    this.$bvModal.hide("modal-save-map");
  }
  //save and export the map in PDF format
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
      this.$bvModal.hide("modal-save-map");
      map.setSize(size);
      map.getView().fit(extent, { size: size });
    });
    let printSize = [width, height];
    map.setSize(printSize);
  }
//save and export the map in shape format
  saveShapefile(titleFile: String) {
    let vectorSource = new Vector();
    let geometry = null;
    let feature = null;
    for (let layer of this.featuresOS) {
      for (let obj of layer) {
        geometry = null;
        if (obj.geometry.type === "Point") {
          geometry = new Point(obj.geometry.coordinates);
        } else {
          geometry = new Polygon(obj.geometry.coordinates);
        }
        feature = new Feature({
          geometry,
          name: obj.properties.name,
        });
        vectorSource.addFeature(feature);
      }
    }
    for (let layer of this.featuresArea) {
      geometry = null;
      if (layer.geometry.type === "Point") {
        geometry = new Point(layer.geometry.coordinates);
      } else {
        geometry = new Polygon(layer.geometry.coordinates);
      }
      feature = new Feature({
        geometry,
        name: layer.properties.name,
      });
      vectorSource.addFeature(feature);
    }
    let writer = new GeoJSON();
    let geojsonStr = writer.writeFeatures(vectorSource.getFeatures());
    let geoJson = JSON.parse(geojsonStr);
    shpwrite.download(geoJson);
    this.$bvModal.hide("modal-save-map");
  }
//Get Events linked with the area
 appendTemporalArea(obj) {
    let minDate = this.$opensilex.prepareGetParameter(this.minDate);
    let maxDate = this.$opensilex.prepareGetParameter(this.maxDate);

    if (minDate != undefined) {
      minDate = minDate.toISOString();
    }
    if (maxDate != undefined) {
      maxDate = maxDate.toISOString();
    }

   if (obj != undefined) {
      this.temporalAreas.push(obj.event);
      this.temporalAreas.sort((a: any, b: any) => {
        return new Date(b.end).getTime() - new Date(a.end).getTime();
      });
    }

  }
//Check selected features and make different actions depending on the number of feature
  updateSelectionFeatures(features) {
    if (features.length && features[0]) {
      this.selectedFeatures = features;
    }
    return this.selectedFeatures.length === 1
      ? this.showDetails(this.selectedFeatures[0], true)
      : "";
  }

// Recovery SO at the map creation
  private recoveryScientificObjects(startDate?, endDate?) {
    this.callSO = false;
    this.featuresOS = [];

    this.$opensilex
      .getService(this.scientificObjectsService)
      .searchScientificObjectsWithGeometryListByUris(
        this.experiment,
        startDate,
        endDate
      )
      .then(
        (
          http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>
        ) => {
          const res = http.response.result as any;
          res.forEach((element) => {
            if (element.geometry != null) {
              element.geometry.properties = {
                uri: element.uri,
                name: element.name,
                type: element.rdf_type,
                nature: "ScientificObjects",
                display: "true",
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
            }
          });
          if (res.length != 0) {
            this.endReceipt = true;
          }
        }
      )
      .catch((e) => {
        this.$opensilex.errorHandler(e);
        this.$opensilex.hideLoader();
      })
      .finally(() => {
        this.defineCenter()
          .catch(() => {
            // In case of OpenLayer error, recall function in 300ms
            setTimeout(() => {
              this.defineCenter();
            }, 300);
          })
          .finally(() => {
            this.$opensilex.hideLoader();
          });
        this.initScientificObjects();
      });
  }

  private scientificObjectsDetails(scientificObjectUri: any) {
    if (scientificObjectUri != undefined) {
      this.detailsSO = false;
      this.$opensilex.disableLoader();
      this.$opensilex
        .getService(this.scientificObjectsService)
        .getScientificObjectDetail(scientificObjectUri, this.experiment)
        .then(
          (
            http: HttpResponse<OpenSilexResponse<ScientificObjectDetailDTO>>
          ) => {
            let result = http.response.result;
            this.selectedFeatures.forEach((item) => {
              if (item.properties.uri === result.uri) {
                item.properties.OS = result;
                this.detailsSO = true;
              }
            });
          }
        )
        .catch(this.$opensilex.errorHandler)
        .finally(() => {
          this.$opensilex.enableLoader();
        });
    }
  }

  private removeFromFeaturesArea(uri, features) {
    features.forEach((item) => {
      const { uri: uriItem } = item.properties;
      let regExp = /area:.+|area#.+/;

      if (
        uri.slice(uri.search(regExp) + 5) ===
        uriItem.slice(uriItem.search(regExp) + 5)
      ) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromTemporalAreas(uri, features) {
    features.forEach((item) => {
      const uriItem = item.uri;
      let regExp = /events:.+|events#.+/;

      if (
        uri.slice(uri.search(regExp) + 5) ==
        uriItem.slice(uriItem.search(regExp) + 5)
      ) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromTemporalAreasByTarget(uri, features) {
    features.forEach((item) => {
      const uriItem = item.targets[0];
      let regExp = /area:.+|area#.+/;

      if (
        uri.slice(uri.search(regExp) + 5) ===
        uriItem.slice(uriItem.search(regExp) + 5)
      ) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromFeatureOS(uri, features, higherLevelFeatures = []) {
    features.forEach((item) => {
      if (item.type === undefined) {
        this.removeFromFeatureOS(uri, item, features);
      } else {
        const { uri: uriItem } = item.properties;

        if (uri === uriItem) {
          if (features.length > 1 || higherLevelFeatures.length === 0) {
            features.splice(features.indexOf(item), 1);
          } else {
            for (let i = 0; i < higherLevelFeatures.length; i++) {
              if (uri === higherLevelFeatures[i][0].properties.uri) {
                higherLevelFeatures.splice(i, 1);
              }
            }
          }
          return; // Stops at the first matching element
        }
      }
    });
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
      coordinateExtent[0] +
        (coordinateExtent[2] - coordinateExtent[0]) * 0.0303111,
      coordinateExtent[3] +
        (coordinateExtent[1] - coordinateExtent[3]) * 0.025747,
    ];
  }

  private calcCenterMap(coordinateExtent) {
    this.centerMap = [
      (coordinateExtent[0] + coordinateExtent[2]) / 2,
      (coordinateExtent[1] + coordinateExtent[3]) / 2,
    ];
  }
  private zoomRestriction() {
    if (this.mapView.$view.getZoom() < 9) {
      this.isDisabled = true;
      this.featuresArea = [];
      this.featuresDevice = [];
    } else {
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

        //For time-line
        let minDate = this.range.from;
        if (minDate === null) {
          minDate = this.minDate;
        }

        let maxDate = this.range.to;
        if (maxDate === null) {
          maxDate = this.maxDate;
        }
        minDate = this.$opensilex.prepareGetParameter(minDate);
        console.debug("minDate", minDate, this.minDate, this.range.from);

      maxDate = this.$opensilex.prepareGetParameter(maxDate);
      console.debug("maxDate", maxDate, this.maxDate, this.range.to);

        let minDateString: string = undefined;
        let maxDateString: string = undefined;

        if (minDate != undefined) {
          minDateString = minDate.toISOString();
        }

        if (maxDate != undefined) {
          maxDateString = maxDate.toISOString();
        }
        this.areaRecovery(geometry, minDateString,maxDateString);
        this.devicesRecovery(geometry);
      }
    }
  }

//Recovery areas (structural|temporal) in  the current map expansion
  private areaRecovery(geometry, minDateString, maxDateString) {

        this.featuresArea = [];
        this.temporalAreas = [];

        this.areaService
         .searchIntersects(
                geometry,
                minDateString,
                maxDateString
            )
            .then((http: HttpResponse<OpenSilexResponse<Array<AreaGetDTO>>>) => {
              const res = http.response.result as any;
              res.forEach((element) => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                    uri: element.uri,
                    name: element.name,
                    type: element.rdf_type,
                    event: element.event,
                    description: element.description,
                    nature: "Area",
                  };
                  let bool = true;
                  for (let area of this.featuresArea) {
                    if (area.properties.uri === element.geometry.properties.uri) {
                      // Check if already exists
                      bool = false;
                    }
                  }
                  if (bool) {
                    this.featuresArea.push(element.geometry);
                    if (element.geometry.properties.type === this.temporalAreaType) {
                      this.appendTemporalArea(element.geometry.properties);
                    }
                  }
                }
              });
            })
            .catch(this.$opensilex.errorHandler)
            .finally(() => {
              this.endReceipt = true;
              this.$opensilex.hideLoader();
            });
  }

  //Recovery devices in  the current map expansion
  private devicesRecovery(geometry) {

      this.featuresDevice = [];


     this.positionService.searchGeospatializedPosition(
              geometry,
              Oeso.DEVICE_TYPE_URI,
              new Date(this.experimentData.start_date).toISOString(),
              this.experimentData.end_date ? new Date(this.experimentData.end_date).toISOString() : undefined
     )
          .then((http: HttpResponse<OpenSilexResponse<Array<TargetPositionCreationDTO>>>) => {
            const res = http.response.result as any;
            res.forEach((element) => {
              if (element.targetPositions[0].position.coordinates != null) {
                 element = {
                   geometry: {
                     coordinates: element.targetPositions[0].position.coordinates.coordinates.values,
                     type: 'Point'
                   },
                   type: "Feature",
                   properties: {
                     uri: element.uri,
                     target: element.targetPositions[0].target,
                     nature: "Device",
                     }
                 };
                this.featuresDevice.push(element);
              }
            });
          })
          .catch(this.$opensilex.errorHandler)
          .finally(() => {
              this.endReceipt = true;
              this.$opensilex.hideLoader();
            });
  }

  private edit(data) {
    let uri = data.item.properties.uri;

    if (data.item.properties.nature === "Area") {
          let form = data.item.properties;
          form.geometry= data.item.geometry;

          if(data.item.properties.type === this.temporalAreaType){
            form.is_structural_area = false;
            form.rdf_type = form.event.rdf_type;
            form.start = form.event.start;
            form.end = form.event.end;
            form.is_instant = form.event.is_instant;
            form.event=null;
          }
          else {
            form.is_structural_area = true;
            form.rdf_type = form.type;
          }
          this.areaForm.showEditForm(form);
    } else {
      this.callSO = true;
      this.scientificObjectURI = uri;
      this.soForm.editScientificObject(uri);
    }
  }
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
.disabled{
  color:grey ;
  background-color: lightgrey;
  pointer-events: none;
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
    Instruction: Press Shift to <b>select item by item</b> on the map. Press and hold Shift + Alt + Click and move the mouse to rotate the map. Press Ctrl + Click while dragging to <b>select multiple scientific objects</b>.
    WarningInstruction: Currently, the selection tool does not follow the rotation.
    details: Show or hide element details
    author: Author
    update: Update element
    display: Layers
    displayFilter: Filters ({count})
    mapPanel: Manage map panel
    mapPanelTitle: Map Panel
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
    save: Save the map
    time: See temporal(s) area(s)
    noFilter: No filter applied. To add one, use the form below the map
    save-confirmation: Do you want to export the map as PNG image or PDF ?
    noTemporalAreas: No temporal areas are displayed on the map.
    marketFrom: From
    marketTo: To
    dateRange: Handle scientific objects with date range
  Area:
    title: Area
    add: Description of the area
    update: Update Area
    display: Areas
  Filter:
    add: Creation of the filter
    update: Update Filter
    created: The filter
  ScientificObjects:
    title: Scientific object
    update: Scientific object has been updated
    display: Scientific objects

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
    Instruction: Appuyez sur Maj pour <b>sélectionner élément par élément</b> sur la carte. Appuyez et maintenez Maj +Alt + Clic puis déplacer la souris pour faire <b>pivoter</b> la carte. Appuyez sur Ctrl + Clic tout en faisant glisser pour <b>sélectionner plusieurs objets scientifiques</b>.
    WarningInstruction: Actuellement, l'outil de sélection ne suit pas la rotation.
    details: Afficher ou masquer les détails de l'élément
    author: Auteur
    update: Mise à jour de l'élément
    display: Couches
    displayFilter: Filtres ({count})
    mapPanel: Gérer la carte
    mapPanelTitle: Contrôle
    eventPanelTitle: Évènements
    eventPanel-help: Le panneau des événements affiche tous les événements liés aux zones temporaires. Ils sont affichés des plus récents aux plus anciens.
    mapPanelScientificObjects: Objets Scientifiques
    mapPanelAreas: Zones
    mapPanelAreasStructuralArea: Zone structurelle
    mapPanelAreasTemporalArea: Zone temporaire
    mapPanelFilters: Filtres
    mapPanelDevices : Capteurs
    create-filter: Créer un filtre
    center: Recentrer la carte
    save: Enregistrer la carte
    time: Visualiser les zones temporaires
    noFilter: Aucun filtre appliqué. Pour en ajouter, utiliser le formulaire situé sous la carte
    save-confirmation: Voulez-vous exporter la carte au format PNG ou PDF ?
    noTemporalAreas: Aucune zone temporaire n'est affichée sur la carte.
    marketFrom: Du
    marketTo: Jusqu'au
    dateRange: Manipuler des objets scientifiques avec une plage de dates
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
</i18n>
