<template>
  <div id="map">
    <div v-if="!editingMode" id="selected" class="d-flex">
      <div class="mr-auto p-2">
        <opensilex-CreateButton
            v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
            label="MapView.add-area-button"
            @click="editingMode = true"
        ></opensilex-CreateButton>
        <!-- map panel, controls -->
        <b-button v-b-toggle.map-sidebar>{{ $t("MapView.mapPanel") }}</b-button>
        <!--// map panel, controls -->
        <opensilex-Button icon="fa#crosshairs" label="MapView.center" @click="defineCenter"></opensilex-Button>
        <opensilex-Button icon="fa#save" label="MapView.save" @click="saveMap"></opensilex-Button>
        <b-modal id="modal-save-map">
          <template #default>
            <p>{{ $t("MapView.save-confirmation") }}</p>
          </template>
          <template #modal-footer>
            <b-button variant="danger" @click="savePDF(titleFile)">PDF</b-button>
            <b-button variant="info" @click="savePNG(titleFile)">PNG</b-button>
            <b-button variant="success" @click="saveShapefile(titleFile)">Shapefile</b-button>
          </template>
        </b-modal>
        <b-button
        v-on:click="showTemporalAreas"
        v-b-toggle.event-sidebar
        :title="$t('MapView.time')"
        >
          <slot name="icon">
            <opensilex-Icon :icon="'ik#ik-activity'" />
          </slot>
          </b-button>
      </div>
      <span class="p-2">
        <label class="alert-warning">
          <img alt="Warning" src="../../../theme/phis/images/construction.png"/>
          {{ $t("MapView.WarningInstruction") }}
        </label>
      </span>
    </div>
    <div v-if="editingMode" id="editing">
      <opensilex-Button
          :small="false"
          icon
          label="MapView.selected-button"
          variant="secondary"
          @click="editingMode = false"
      ></opensilex-Button>
    </div>
    <opensilex-ModalForm
        v-if="!errorGeometry"
        ref="areaForm"
        :successMessage="successMessageArea"
        component="opensilex-AreaForm"
        createTitle="Area.add"
        editTitle="Area.update"
        icon="fa#sun"
        modalSize="lg"
        @onCreate="showAreaDetails"
        @onUpdate="callAreaUpdate"
    ></opensilex-ModalForm>
    <opensilex-ScientificObjectForm
        v-if=" user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
        ref="soForm"
        :context="{ experimentURI: this.experiment }"
        @onCreate="callScientificObjectUpdate"
        @onUpdate="callScientificObjectUpdate"
    />

    <div id="mapPoster" :class="editingMode ? 'bg-light border border-secondary' : ''">
      <p class="alert-info">
        <span v-if="!editingMode" v-html="$t('MapView.Instruction')"></span>
      </p>
      <!-- "mapControls" to display the scale -->
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
      >
        <vl-view
            ref="mapView"
            :min-zoom="2"
            :zoom="3"
            @update:rotation="areaRecovery"
            @update:zoom="areaRecovery"
            @update:center="overlayPositionsRecovery"
        ></vl-view>

        <vl-layer-tile id="osm">
          <vl-source-osm :wrap-x="false"/>
        </vl-layer-tile>

        <!-- position ternary to show the overlay only if mouse on Object else default value -->
        <!-- stop-event property to be sure the map is not glitching vertically -->
        <vl-overlay
            id="overlay"
            :position="overlayCoordinate.length === 2 ? overlayCoordinate : [0, 0]"
            :stop-event="selectPointerMove.name && selectPointerMove.type ? true : false">
          <template slot-scope="scope">
            <div
                class="panel-content"
            >{{ displayInfoInOverlay() }}
            </div>
          </template>
        </vl-overlay>

        <vl-overlay
            id="detailItem"
            :position="centerMap.length === 2 ? centerMap : [0, 0]"
        >
          <template slot-scope="scope">
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

        <template v-if="endReceipt">
          <vl-layer-vector
            v-for="area in featuresArea"
            :key="area.id"
            :visible="isAreaVisible(area)"
          >
            <div v-if="area.properties.type != temporalAreaType">
              <vl-source-vector ref="vectorSourceArea" :features="[area]"></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="green"></vl-style-stroke>
                <vl-style-fill color="rgba(200,255,200,0.4)"></vl-style-fill>
              </vl-style-box>
            </div>
            <div v-if="area.properties.type == temporalAreaType && !isSelectedArea(area)">
              <vl-source-vector ref="vectorSourceArea" :features="[area]"></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="red"></vl-style-stroke>
                <vl-style-fill color="rgba(128,139,150,0.4)"></vl-style-fill>
              </vl-style-box>
            </div>
            <div v-if="area.properties.type == temporalAreaType && isSelectedArea(area)">
              <vl-source-vector ref="vectorSourceArea" :features="[area]"></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="#33A0CC" :width="3"></vl-style-stroke>
                <vl-style-fill color="rgba(255,255,255,0.6)"></vl-style-fill>
              </vl-style-box>
            </div>
          </vl-layer-vector>
          <vl-layer-vector
              v-for="layerSO in featuresOS"
              :key="layerSO.id"
          >
            <vl-source-vector ref="vectorSource" :features="layerSO"></vl-source-vector>
          </vl-layer-vector>
          <vl-layer-vector
              v-for="layer in tabLayer"
              :key="layer.ref"
              :visible="displayFilters && layer.display === 'true'"
          >
            <vl-source-vector :ref="layer.ref" :features.sync="layer.tabFeatures"></vl-source-vector>
            <vl-style-box>
              <vl-style-stroke v-if="layer.vlStyleStrokeColor" :color="layer.vlStyleStrokeColor"></vl-style-stroke>
              <!-- outline color -->
              <vl-style-fill
                  v-if="layer.vlStyleFillColor"
                  :color="colorFeature(layer.vlStyleFillColor)"
              ></vl-style-fill>
              <vl-style-circle :radius="5">
                <vl-style-stroke v-if="layer.vlStyleStrokeColor" :color="layer.vlStyleStrokeColor"></vl-style-stroke>
                <!-- outline color -->
                <vl-style-fill
                    v-if="layer.vlStyleFillColor"
                    :color="colorFeature(layer.vlStyleFillColor)"
                ></vl-style-fill>
              </vl-style-circle>
            </vl-style-box>
          </vl-layer-vector>
        </template>

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
              <vl-style-box>
                <vl-style-stroke color="blue"></vl-style-stroke>
                <vl-style-fill color="rgba(255,255,255,0.5)"></vl-style-fill>
              </vl-style-box>
            </vl-interaction-draw>
          </div>
        </template>

        <!-- to make the selection -->
        <vl-interaction-select
            v-if="!editingMode"
            id="select"
            ref="selectInteraction"
            :features="selectedFeatures"
            @update:features="updateSelectionFeatures"
        />
      </vl-map>
    </div>

    <b-sidebar id="event-sidebar" v-model="timelineSidebarVisibility" width="400px" right class="sidebar-content" no-header shadow>
      <template #default="{ hide }">
        <div class="b-sidebar-header header-brand hamburger-container opensilex-sidebar-header">
          <div class="d-flex">
            <span id="map-sidebar___title__" class="text mr-auto p-3">
              {{ $t('MapView.eventPanelTitle').toUpperCase() }}
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
        <div v-if="temporalAreas.length == 0" class="p-3">
          <p>{{ $t('MapView.noTemporalAreas') }}</p>
        </div>
        <opensilex-Timeline
          :items="temporalAreas"
          :selectedFeatures="selectedFeatures"
          @onClick="selectFeaturesFromTimeline"
          >
        </opensilex-Timeline>
      </template>
    </b-sidebar>

    <b-sidebar id="map-sidebar" visible no-header class="sidebar-content">
      <template #default="{ hide }">
        <div class="b-sidebar-header header-brand hamburger-container opensilex-sidebar-header">
          <div class="d-flex">
            <span id="map-sidebar___title__" class="text mr-auto p-3">{{ $t('MapView.mapPanelTitle').toUpperCase() }}</span>
            <button class="hamburger hamburger--collapse is-active p-3" @click="hide">
              <span class="hamburger-box">
                <span class="hamburger-inner"></span>
              </span>
            </button>
          </div>
        </div>
        <b-tabs content-class="mt-3">

          <opensilex-TreeView :nodes.sync="scientificObjects">
            <template v-slot:node="{ node }">
              <span class="item-icon">
              </span>&nbsp;
              <span v-if="node.title == 'Scientific Object'">{{ $t('MapView.mapPanelScientificObjects') }} ({{ getNumberScientificObjects(featuresOS) }})</span>
              <span v-else>{{ getType(node.data.properties.type) }} ({{ getNumberScientificObjectsByType(getType(node.data.properties.type)) }})</span>
            </template>

            <template v-slot:buttons="{ node }">
              <opensilex-CheckboxForm
                :value.sync="displaySO"
                class="col-lg-2"
                v-if="node.title == 'Scientific Object'"
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

          <opensilex-TreeView :nodes.sync="areas">
            <template v-slot:node="{ node }">
              <span class="item-icon">
              </span>&nbsp;
              <span v-if="node.title == 'Areas'">{{ $t('MapView.mapPanelAreas') }} ({{ featuresArea.length }})</span>
              <span v-else>{{ $t('MapView.mapPanelAreas' + node.title) }} ({{ getNumberByZone(node.title) }})</span>
            </template>

            <template v-slot:buttons="{ node }">

              <opensilex-CheckboxForm
                v-if="node.title == 'Areas'"
                :value.sync="displayAreas"
                class="col-lg-2"
                :small="true"
                @update:value="updateArea(node)"
              ></opensilex-CheckboxForm>
              <opensilex-CheckboxForm
                :disabled="!displayAreas"
                v-if="node.title == 'PerennialZone'"
                :value.sync="displayPerennialAreas"
                class="col-lg-2"
                @update:value="updateArea(node)"
                :small="true"
              ></opensilex-CheckboxForm>
              <opensilex-CheckboxForm
                :disabled="!displayAreas"
                v-if="node.title == 'TemporalZone'"
                :value.sync="displayTemporalAreas"
                class="col-lg-2"
                @update:value="updateArea(node)"
                :small="true"
              ></opensilex-CheckboxForm>
            </template>
          </opensilex-TreeView>

          <opensilex-TreeView :nodes.sync="filters">
            <template v-slot:node="{ node }">
              <span class="item-icon">
              </span>&nbsp;
                <span v-if="node.title == 'Filters'">{{ $t('MapView.mapPanelFilters') }} ({{ tabLayer.length }})</span>
                <span class="p-2 bd-highlight" v-else>{{ node.title }}</span>
            </template>

            <template v-slot:buttons="{ node }">

              <opensilex-CheckboxForm
              :disabled="!tabLayer.length"
              v-if="node.title == 'Filters'"
              :value.sync="displayFilters"
              class="col-lg-2"
              :small="true"
            ></opensilex-CheckboxForm>
              <div class="d-flex flex-row mx-auto" v-if="node.title != 'Filters'">
                <opensilex-CheckboxForm
                v-if="node.title != 'Filters'"
                :value="true"
                @update:value="updateFilterDisplay(node)"
                class="align-self-center"
                :small="true"
                :disabled="!displayFilters"
                ></opensilex-CheckboxForm>
                <opensilex-InputForm style="width: 35px !important;"
                  v-if="node.data.vlStyleStrokeColor"
                  type="color"
                  :value.sync="node.data.vlStyleStrokeColor"
                  @update:value="updateColorFilter(node, 'vlStyleStrokeColor')"
                class="align-self-center"
                ></opensilex-InputForm>
                <opensilex-InputForm style="width: 35px !important;"
                  v-if="node.data.vlStyleFillColor"
                  type="color"
                  :value.sync="node.data.vlStyleFillColor"
                  @update:value="updateColorFilter(node, 'vlStyleFillColor')"
                class="align-self-center"
                ></opensilex-InputForm>
                <opensilex-DeleteButton
                  label="FilterMap.filter.delete-button"
                  :small="true"
                  @click="tabLayer.forEach((element, index) => { if (element.titleDisplay == node.data.titleDisplay) tabLayer.splice(index, 1) })"
                  class="align-self-center"
                ></opensilex-DeleteButton>
              </div>
            </template>
          </opensilex-TreeView>
          <opensilex-CreateButton
            class="ml-50 mt-10"
            label="MapView.create-filter"
            @click="filterForm.showCreateForm();"
        ></opensilex-CreateButton>
        </b-tabs>
      </template>
    </b-sidebar>
    {{ $t("MapView.Legend") }}:
    <span id="OS">{{ $t("MapView.LegendSO") }}</span>
    &nbsp;-&nbsp;
    <span id="PerennialArea">{{ $t("MapView.LegendPerennialArea") }}</span>
    &nbsp;-&nbsp;
    <span id="TemporalArea">{{ $t("MapView.LegendTemporalArea") }}</span>
    <div class="timeline-slider" v-if="min != null && max != null">
      <JqxRangeSelector
        ref="JqxRangeSelector"
        class="mx-auto"
        width="75%"
        padding="35px"
        height="15"
        :min="min"
        :max="max"
        :range="range"
        :labelsOnTicks="false"
        majorTicksInterval="day"
        minorTicksInterval="hour"
        labelsFormat="ddd"
        theme="dark"
        :markersFormatFunction=markersDateRangeFormat
        @change="onChangeDateRange($event)"
        >
      </JqxRangeSelector>
    </div>
    <div id="selectedTable" v-if="selectedFeatures.length !== 0" class="selected-features" >
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

        <template v-slot:cell(type)="{ data }">{{ nameType(data.item.properties.type) }}</template>

        <template v-slot:row-details="{ data }">
          <opensilex-DisplayInformationAboutItem
              :details-s-o="detailsSO"
              :experiment="experiment"
              :item="data.item"
          />
        </template>

        <template v-slot:cell(actions)="{ data }">
          <b-button-group size="sm">
            <div v-if="user.admin === true">
              <opensilex-DetailButton
                  :detailVisible="data['detailsShowing']"
                  :small="true"
                  label="MapView.details"
                  @click="showDetails(data)"
              ></opensilex-DetailButton>

              <opensilex-EditButton
                  v-if=" data.item.properties.nature === 'Area'
                    ? user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)
                    : user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID)"
                  :small="true"
                  label="MapView.update"
                  @click="edit(data)"
              ></opensilex-EditButton>

              <opensilex-DeleteButton
                  v-if=" data.item.properties.nature === 'Area'
                    ? user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)
                    : user.hasCredential(credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID)"
                  label="MapView.delete-button"
                  @click="selectedFeatures.splice(selectedFeatures.indexOf(data.item),1) && deleteItem(data)"
              ></opensilex-DeleteButton>
            </div>
          </b-button-group>
        </template>
      </opensilex-TableView>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {DragBox} from "ol/interaction";
import { GeoJSON } from "ol/format";
import { Vector } from 'ol/source';
import Collection from 'ol/Collection';
import Select from 'ol/interaction/Select';
import Feature from 'ol/Feature';
import * as olExtent from 'ol/extent';
import Polygon from 'ol/geom/Polygon';
import Point from 'ol/geom/Point';
import {platformModifierKeyOnly} from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
let shpwrite = require('shp-write');
// @ts-ignore
import {ScientificObjectNodeDTO} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {transformExtent} from "vuelayers/src/ol-ext/proj";
// @ts-ignore
import {AreaGetDTO} from "opensilex-core/model/areaGetDTO";
// @ts-ignore
import { EventGetDTO } from 'opensilex-core/model/eventGetDTO';
// @ts-ignore
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
// @ts-ignore
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {defaults, ScaleLine} from "ol/control";
import Oeso from "../../ontologies/Oeso";
import * as turf from "@turf/turf";
import { jsPDF } from "jspdf"
import { saveAs } from 'file-saver';
// @ts-ignore
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";

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
  el: "map";
  service: any;
  featuresOS: any[] = [];
  featuresArea: any[] = [];
  temporaryArea: any[] = [];
  temporalAreas: any[] = [];
  selectPointerMove: { name: string, type: string } = { name: null, type: null };
  overlayCoordinate: any[] = [];
  centerMap: any[] = [];
  tabLayer: any[] = [];
  selectInteraction: any = null;
  collection: any = null;
  fieldsSelected = [
    {
      key: "name",
      label: "MapView.name"
    },
    {
      key: "type",
      label: "MapView.type"
    },
    {
      key: "actions",
      label: "actions"
    }
  ];
  selectedFeatures: any[] = [];
  timelineSidebarVisibility: boolean = false;
  min: Date = null;
  max: Date = null;
  range: { from: Date, to: Date } = { from: null, to: null };

  private editingMode: boolean = false;
  private displayAreas: boolean = true;
  private displayPerennialAreas: boolean = false;
  private displayTemporalAreas: boolean = false;
  private displayFilters: boolean = true;
  private temporalAreaType: String = "vocabulary:TemporalArea";
  private perennialAreaType: String = "vocabulary:Area";
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
  private areaService = "opensilex.AreaService";
  private eventsService = "EventsService";
  private scientificObjectURI: string;
  private scientificObjects: any = [];
  private areas: any = [{
    "title": "Areas",
    "isLeaf": false,
    "children": [
      {
        "title": "PerennialZone",
        "isLeaf": true,
        "isSelectable": false,
        "isDraggable": false,
        "isCheckable": true,
        "isExpanded": false,
        "isSelected": null,
        "children": []
      },
      {
        "title": "TemporalZone",
        "isLeaf": true,
        "isSelectable": false,
        "isDraggable": false,
        "isCheckable": true,
        "isExpanded": false,
        "isSelected": null,
        "children": []
      }
    ],
    "isExpanded": true,
    "isSelected": null,
    "isDraggable": false,
    "isSelectable": false,
    "isCheckable": true
  }];
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
    return this.getType(this.$attrs.uri) + '-map';
  }

  get filters() {
    let result = [];
    let filt = {
        "title": "Filters",
        "data": {
          "isCheckable": true,
          "name": "Filters",
          "selected": false,
          "disabled": false,
        },
        "isLeaf": false,
        "children": [],
        "isExpanded": true,
        "isSelected": null,
        "isDraggable": false,
        "isSelectable": false,
        "isCheckable": true
    };

    for (let filter of this.tabLayer) {
      let tmp: any = {
        title: filter.titleDisplay,
        isLeaf: true,
      };
      tmp.isSelectable = false
      tmp.isDraggable = false
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

  set filters(value) { }

  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.rangeSelector.refresh(); // Refresh date range
        this.rangeSelector.setRange(this.range.from, this.range.to);
      }
    );
    this.initDateRange();
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  displayInfoInOverlay(): string {
    if (this.selectPointerMove.name && this.selectPointerMove.type) { // if Object existing and filled
      return this.selectPointerMove.name + " (" + this.selectPointerMove.type + ")";
    } else {
      return '';
    }
  }

  getNumberScientificObjects(featureOs): Number {
    let res = 0;

    for (let layer of featureOs) {
      res += layer.length
    }
    return res;
  }

  getNumberScientificObjectsByType(type: String): Number {
    let res = 0;

    for (let layer of this.featuresOS) {
      for (let obj of layer) {
        if (this.getType(obj.properties.type) == type) {
          res += 1;
        }
      }
    }
    return res;
  }

  getNumberByZone(zoneType: String): Number {
    if (zoneType == 'PerennialZone') {
      return this.getNumberPerennialZone();
    } else {
      return this.getNumberTemporalZone();
    }
  }

  getNumberPerennialZone(): Number {
    let res = 0;

    for (let area of this.featuresArea) {
      if (area.properties.type != this.temporalAreaType) {
        res += 1;
      }
    }
    return res;
  }

  getNumberTemporalZone(): Number {
    let res = 0;

    for (let area of this.featuresArea) {
      if (area.properties.type == this.temporalAreaType) {
        res += 1;
      }
    }
    return res;
  }

  updateFilterDisplay(node) {
    for (let layer of this.tabLayer) {
      if (layer.titleDisplay == node.title) {
        if (layer.display == "false") {
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
    this.tabLayer.forEach(layer => {
      if (layer.titleDisplay == node.title) {
        if (type == 'vlStyleStrokeColor') {
          layer.vlStyleStrokeColor = node.data.vlStyleStrokeColor;
        } else {
          layer.vlStyleFillColor = node.data.vlStyleFillColor;
        }
        return;
      }
    });
  }

  updateArea(node) { // Update the visibility of areas
    const type: 'Areas' | 'PerennialZone' | 'TemporalZone' = node.title; // What node is clicked ?
    const layers = this.map.$map.getLayers();
    layers.forEach((element, index) => { // Iterate all layers of the map (OpenLayers API) to find the ones that are linked to ScientificObjects
      const source = element.getSource();
      if (source) {
        const collection = source.featuresCollection_;
        if (collection) {
          const array = collection.array_; // Get array of Features
          if (array && array.length > 0) {
            // We can check only the first element of the array
            // instead of check all elements (optimisation)
            if (array[0].values_.nature == "Area" && // Element must be of type 'Area'
            (type == 'Areas' || // If 'Areas' node is clicked
            (type == 'PerennialZone' && this.getType(array[0].values_.type) != 'TemporalArea') // If 'PerennialZone' node is clicked and element is not temporal area type
            || (type == 'TemporalZone' && this.getType(array[0].values_.type) == 'TemporalArea'))) { // If 'TemporalZone' node is clicked and element is temporal area type
              let status: boolean = false;
              if (type == 'Areas') {
                status = this.displayAreas;
                if (status) { // Trick to save or get the last visible status
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

  updateScientificObject(node) { // Update the Scientific Objects of areas
    const isAllScientificObjects: boolean = node.title == 'Scientific Object'; // is the 'Scientific Object' node clicked ?
    const layers = this.map.$map.getLayers();
    layers.forEach((element, index) => { // Iterate all layers of the map (OpenLayers API) to find the ones that are linked to ScientificObjects
      const source = element.getSource();
      if (source) {
        const collection = source.featuresCollection_;
        if (collection) {
          const array = collection.array_; // Get array of Features
          if (array && array.length > 0) {
            // We can check only the first element of the array
            // instead of check all elements (optimisation)
            if (array[0].values_.nature == "ScientificObjects" && (isAllScientificObjects // All Scientific Objects
            || array[0].values_.name == node.title)) { // Only the only type selected
              let status: boolean = false;
              if (isAllScientificObjects) {
                status = this.displaySO;
                if (status) { // Trick to save or get the last visible status
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
    if (res.hash != '') { // long type (example: http://opensilex.dev/set/area#youpi)
      return res.hash;
    } else { // short type (example: Vocabulary#Area)
      return res.pathname;
    }
  }

  isAreaVisible(area) {
    if (area.properties.type != this.temporalAreaType) {
      return this.displayPerennialAreas;
    } else if (area.properties.type != this.perennialAreaType) {
      return this.displayTemporalAreas;
    }
  }

  initScientificObjects() {
    let scientificObject: any = {
      "title": "Scientific Object",
      "data": {
        "isCheckable": true,
        "name": "Scientific Object",
        "selected": false,
        "disabled": false,
      },
      "isLeaf": false,
      "children": [],
      "isExpanded": true,
      "isSelected": null,
      "isDraggable": false,
      "isSelectable": false,
      "isCheckable": true
    };

    this.featuresOS.forEach(item => {
      if (item[0].properties.name) {
        item[0].title = item[0].properties.name
        item[0].isLeaf = true
        item[0].isSelectable = false
        item[0].isDraggable = false
        item[0].isCheckable = true;
        item[0].isExpanded = false;
        item[0].isSelected = null;
        item[0].data = {
          "children": [],
          "name": item[0].properties.name,
          "disabled": false,
          "selected": false,
        }
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
        if (feature[0].properties.display == "true") {
          feature[0].properties.display = "false";
          this.subDisplaySO.push(feature[0].properties.uri);
        }
      }
    } else {
      for (const feature of this.featuresOS) {
        for (const uri of this.subDisplaySO) {
          if (feature[0].properties.uri == uri) {
            feature[0].properties.display = "true";
            this.subDisplaySO.splice(this.subDisplaySO.indexOf(uri), 1);
          }
        }
      }
    }
  }

  isSelectedArea(area) {
    for (let selected of this.selectedFeatures) {
      if (selected.properties.uri == area.properties.uri) {
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

  showAreaDetails(areaUriResult: any) {
    if (areaUriResult instanceof Promise) {
      areaUriResult.then(areaUri => {
        this.recoveryShowArea(areaUri);
      });
    } else {
      this.recoveryShowArea(areaUriResult);
    }
  }

  showFiltersDetails(filterResult: any) {
    if (filterResult.ref) {
      this.tabLayer.forEach((element, index) => {
        if (element.ref == filterResult.ref) {
          this.tabLayer.splice(index, 1)
        }
      })
      this.tabLayer.push(filterResult);
    }
  }

  onMapPointerMove({pixel}: any) {
    // Gets the name when the cursor hovers over the item.
    const hitFeature = this.map.forEachFeatureAtPixel(
        pixel,
        feature => feature
    );
    if (hitFeature) {
      this.selectPointerMove = {
        name: hitFeature.values_.name,
        type: this.nameType(hitFeature.values_.type)
      };
    } else {
      this.selectPointerMove = { name: null, type: null};
    }
  }

  callAreaUpdate(areaUriResult) {
    if (areaUriResult instanceof Promise) {
      areaUriResult.then(areaUri => {
        this.recoveryArea(areaUri);
      });
    } else {
      this.recoveryArea(areaUriResult);
    }
  }

  markersDateRangeFormat(value: string, position: string) {
    const date = new Date(value);
    let res: string = "";
    
    if (position == 'left')
      res += this.$i18n.t("MapView.marketFrom");
    else
      res += this.$i18n.t("MapView.marketTo");
    res += ": ";
    if (this.$store.getters.language == 'en') {
      res += String(date.getMonth() + 1).padStart(2, '0') + "/" + String(date.getDate()).padStart(2, '0');
    } else {
      res += String(date.getDate()).padStart(2, '0') + "/" + String(date.getMonth() + 1).padStart(2, '0');
    }
    res += "/" + date.getFullYear();
    return "<span>" + res + "<span>";
  }

  onChangeDateRange(event) {
    this.$opensilex.showLoader();
    this.range = { from: event.args.from, to: event.args.to }

    let startDate: string = this.formatDate(this.range.from);
    let endDate: string = this.formatDate(this.range.to);
    this.recoveryScientificObjects(startDate, endDate);
  }

  formatDate(date): string {
    let d = new Date(date),
        month = String(d.getMonth() + 1).padStart(2, '0'),
        day = String(d.getDate()).padStart(2, '0'),
        year = d.getFullYear();

    return [year, month, day].join('-');
  }

  calcDifferenceDateInDays(from: Date, to: Date): number {
    let diffInTime = to.getTime() - from.getTime();
    let diffInDays = diffInTime / (1000 * 3600 * 24);

    return diffInDays;
  }

  majorTicksIntervalFct() {
    const from = this.min;
    const to = this.max;

    if (this.calcDifferenceDateInDays(from, to) > 40) {
      return 'month';
    } else {
      return 'day';
    }
  }

  minorTicksIntervalFct() {
    const from = this.min;
    const to = this.max;

    if (this.calcDifferenceDateInDays(from, to) > 40) {
      return 'day';
    } else {
      return 'hour';
    }
  }

  labelsFormatFct() {
    const from = this.min;
    const to = this.max;

    if (this.calcDifferenceDateInDays(from, to) > 40) {
      return 'MMM';
    } else {
      return 'ddd';
    }
  }

  selectFeaturesFromTimeline(uri) {
    for (let area of this.featuresArea) {
      if (area.properties.uri == uri) {
        for (let i = 0; i < this.selectedFeatures.length; i++) {
          if (this.selectedFeatures[i].properties.uri == uri) {
            this.selectedFeatures = [];
            return;
          }
        }
        this.selectedFeatures = [area];
        return;
      }
    }
  }
  
  callScientificObjectUpdate() {
    if (this.callSO) {
      this.callSO = false;
      this.removeFromFeatureOS(this.scientificObjectURI, this.featuresOS);
      this.removeFromFeatureOS(this.scientificObjectURI, this.selectedFeatures);

      this.$opensilex
          .getService(this.scientificObjectsService)
          .getScientificObjectDetail(this.scientificObjectURI, this.experiment)
          .then((http: HttpResponse<OpenSilexResponse<ScientificObjectDetailDTO>>) => {
                const result = http.response.result as any;

                if (result.geometry != null) {
                  result.geometry.properties = {
                    uri: result.uri,
                    name: result.name,
                    type: result.rdf_type,
                    nature: "ScientificObjects",
                    display: "true"
                  };

                  let inserted = false;
                  this.featuresOS.forEach(item => {
                    if (item[0].properties.type == result.rdf_type) {
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

  memorizesArea() {
    if (this.temporaryArea.length) {
      // Transfers geometry to the form using the $store
      this.$store.state.zone = this.temporaryArea.pop();

      let areaFeature = this.$store.state.zone;

      if (areaFeature.geometry.type === "Polygon") {
        let kinkedPoly = turf.polygon(areaFeature.geometry.coordinates);
        let unKinkedPoly = turf.unkinkPolygon(kinkedPoly);

        if (unKinkedPoly.features.length > 1) {
          let coordinates = [];

          unKinkedPoly.features.forEach(item => {
            coordinates.push(item.geometry.coordinates);
          });

          this.$store.state.zone.geometry.type = "MultiPolygon";
          this.$store.state.zone.geometry.coordinates = coordinates;
        }
      }

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

  showCreateForm() {
    this.areaForm.showCreateForm();
  }

  showTemporalAreas() {
    if (this.displayAreas && !this.displayTemporalAreas && !this.timelineSidebarVisibility) { // show temporal areas if clicking events panel
      this.displayTemporalAreas = true;
      this.updateArea({ title: 'TemporalZone' });
    }
  }

  created() {
    this.$opensilex.showLoader();

    this.experiment = decodeURIComponent(this.$route.params.uri);

    this.retrievesNameOfType();
    this.recoveryScientificObjects();
  }

  getRangeDatesOfExperiment(): Promise<any> {
    return new Promise ((resolve) => {
      this.$opensilex
      .getService("opensilex.ExperimentsService")
      .getExperiment(this.experiment)
      .then(http => {
        let res = http.response.result;
        this.min = new Date(res.start_date);
        this.min.setHours(0,0,0,0);
        if (res.end_date) {
          this.max = new Date(res.end_date);
        } else {
          this.max = new Date();
        }
        this.max.setHours(0,0,0,0);
        this.range = { from: this.min, to: this.max };
        resolve("");
      })
      .catch(this.$opensilex.errorHandler);
    });
  }

  configDateRange() {
    this.rangeSelector.min = this.min;
    this.rangeSelector.max = this.max;
    this.rangeSelector.range = this.range;
    this.rangeSelector.majorTicksInterval = this.majorTicksIntervalFct();
    this.rangeSelector.minorTicksInterval = this.minorTicksIntervalFct();
    this.rangeSelector.labelsFormat = this.labelsFormatFct();
    this.rangeSelector.range = { from: this.range.from, to: this.range.to };
    this.rangeSelector.refresh();
  }

  initDateRange() {
    // Recover start and end of the experiment
    if (!this.min || !this.max || !this.range.from || !this.range.to) {
      this.getRangeDatesOfExperiment()
      .then(() => {
        this.configDateRange();
      });
    } else {
      this.configDateRange();
    }
  }

  mapCreated(map) {
    // a DragBox interaction used to select features by drawing boxes
    const dragBox = new DragBox({
      condition: platformModifierKeyOnly,
      onBoxEnd: () => {
        // features that intersect the box are selected
        if (this.isMapHasLayer) {
          const extent = dragBox.getGeometry().getExtent();
          if (this.$refs.vectorSource) {
            (this.$refs.vectorSource as any).forEach(vector => {
              const source = vector.$source;
  
              source.forEachFeatureIntersectingExtent(extent, (feature: any) => {
                feature = olExt.writeGeoJsonFeature(feature);
                this.selectedFeatures.push(feature);
              });
            });
          }
        }
      }
    });

    map.$map.addInteraction(dragBox);

    this.map.$map.on('click', (event) => {
      let isFeatureSelected = this.map.$map.getFeaturesAtPixel(event.pixel);
      if (!isFeatureSelected && !this.editingMode) {
        this.selectedFeatures = [];
      }
    });

    // clear selection when drawing a new box and when clicking on the map
    dragBox.on("boxStart", () => {
      this.selectedFeatures = [];
    });
  }

  successMessageArea() {
    return this.$i18n.t("MapView.label");
  }

  private waitFor(conditionFunction) {

    const poll = resolve => {
      if(conditionFunction()) resolve();
      else setTimeout(_ => poll(resolve), 400);
    }

    return new Promise(poll);
  }

  defineCenter(): Promise<boolean> {
    return new Promise((resolve) => {
    if (this.featuresOS.length > 0) {
      this.waitFor(_ => this.vectorSource.length === this.featuresOS.length && this.vectorSource[0].$source) // Wait all vectors charged
      .then(() => {
      let extent = olExtent.createEmpty();
        for (let vector of this.vectorSource) {
          if (vector && vector.$source) {
        let extentTemporary = vector.$source.getExtent();
        olExtent.extend(extent, extentTemporary);
          }
        }
      this.mapView.$view.fit(extent);
          resolve(true);
      })
      } else {
        resolve(false);
    }
    })
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
      if (typeLabelElement.uri == uriType) return typeLabelElement.name;
    }
  }

  deleteItem(data) {
    let uri = data.item.properties.uri;

    if (data.item.properties.nature === "Area") {
      this.$opensilex
          .getService(this.areaService)
          .deleteArea(uri)
          .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
            if (data.item.properties.type == "vocabulary:TemporalArea") {
              return this.deleteEvent(uri);
            }
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
          .then(http => {
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
    return "rgba(" + parseInt(color.slice(1, 3), 16) + "," + parseInt(color.slice(3, 5), 16) + "," + parseInt(color.slice(5, 7), 16) + ",0.5)"
  }

  saveMap() {
    this.$bvModal.show('modal-save-map');
  }

  savePNG(titleFile: String) {
    let canvas = document.getElementsByTagName('canvas')[0];
    canvas.toBlob((blob) => {
      saveAs(blob, titleFile + '.png');
    });
    this.$bvModal.hide('modal-save-map');
  }

  savePDF(titleFile: String) {
    let map = this.map.$map;
    let size = map.getSize();
    let dim = [size[0], size[1]];
    let resolution = 32;
    let width = Math.round(dim[0] * resolution / 25.4);
    let height = Math.round(dim[1] * resolution / 25.4);
    let extent = map.getView().calculateExtent(size);
    map.once('rendercomplete', (event) => {
      let canvas = event.context.canvas;
      let data = canvas.toDataURL('image/jpeg');
      let pdf = new jsPDF({
        orientation: 'landscape',
        unit: 'px',
        format: [dim[0], dim[1]]
      });
      pdf.addImage(data, 'JPEG', 0, 0, dim[0], dim[1]);
      pdf.save(titleFile + '.pdf');
      this.$bvModal.hide('modal-save-map');
      map.setSize(size);
      map.getView().fit(extent, { size: size });
    });
    let printSize = [width, height];
    map.setSize(printSize);
  }

  saveShapefile(titleFile: String) {
    let vectorSource = new Vector();
    let geometry = null;
    let feature = null;
    for (let layer of this.featuresOS) {
      for (let obj of layer) {
        geometry = null;
        if (obj.geometry.type == 'Point') {
          geometry = new Point(obj.geometry.coordinates);
        } else {
          geometry = new Polygon(obj.geometry.coordinates);
        }
        feature = new Feature({
          geometry,
          name: obj.properties.name,
        });
        vectorSource.addFeature(feature)
      }
    }
    for (let layer of this.featuresArea) {
      geometry = null;
      if (layer.geometry.type == 'Point') {
        geometry = new Point(layer.geometry.coordinates);
      } else {
        geometry = new Polygon(layer.geometry.coordinates);
      }
      feature = new Feature({
        geometry,
        name: layer.properties.name
      });
      vectorSource.addFeature(feature)
    }
    let writer = new GeoJSON();
    let geojsonStr = writer.writeFeatures(vectorSource.getFeatures());
    let geoJson = JSON.parse(geojsonStr);
    shpwrite.download(geoJson);
    this.$bvModal.hide('modal-save-map');
  }

  appendTemporalArea(obj) {
    this.$opensilex
      .getService(this.eventsService)
      .searchEvents(undefined, undefined, undefined, obj.uri)
      .then((http: HttpResponse<OpenSilexResponse<EventGetDTO>>) => {
        const res = http.response.result[0] as any;
        res.targets = [obj.uri];
        this.temporalAreas.push(res);
        this.temporalAreas.sort((a: any, b: any) => {
          return new Date(b.end).getTime() - new Date(a.end).getTime();
        })
      })
      .catch(this.$opensilex.errorHandler);
  }

  updateSelectionFeatures(features) {
    if (features.length && features[0]) {
      this.selectedFeatures = features;
    }
    return this.selectedFeatures.length === 1 ? this.showDetails(this.selectedFeatures[0], true) : '';
  }

  private recoveryShowArea(areaUri) {
    if (areaUri != undefined) {
      this.editingMode = false;
      this.$opensilex
          .getService(this.areaService)
          .getByURI(areaUri.toString())
          .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
            const res = http.response.result as any;
            if (res.geometry != null) {
              res.geometry.properties = {
                uri: res.uri,
                name: res.name,
                type: res.rdf_type,
                nature: "Area"
              };
              for (let area of this.featuresArea) {
                if (area.properties.uri == res.geometry.properties.uri) { // Check if already exists
                  return;
                }
              }
              this.featuresArea.push(res.geometry);
              if (res.geometry.properties.type == this.temporalAreaType) {
                this.appendTemporalArea(res.geometry.properties);
                this.displayTemporalAreas = true;
              } else {
                this.displayPerennialAreas = true;
              }
              return;
            }
          })
          .catch(this.$opensilex.errorHandler);
    }
  }

  private recoveryArea(areaUri) {
    if (areaUri != undefined) {
      this.removeFromFeaturesArea(areaUri, this.featuresArea);
      this.removeFromFeaturesArea(areaUri, this.selectedFeatures);
      this.removeFromTemporalAreasByTarget(areaUri, this.temporalAreas);

      this.$opensilex
          .getService(this.areaService)
          .getByURI(areaUri)
          .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
            const res = http.response.result as any;
            if (res.geometry != null) {
              res.geometry.properties = {
                uri: res.uri,
                name: res.name,
                type: res.rdf_type,
                nature: "Area"
              };
              for (let area of this.featuresArea) {
                if (area.properties.uri == res.geometry.properties.uri) { // Check if already exists
                  return;
                }
              }
              this.featuresArea.push(res.geometry);
              if (res.geometry.properties.type == this.temporalAreaType) {
                this.appendTemporalArea(res.geometry.properties);
              }
              this.selectedFeatures.push(res.geometry);
            }
          })
          .catch(this.$opensilex.errorHandler);
    }
  }

  private recoveryScientificObjects(startDate?, endDate?) {
    this.callSO = false;
    this.featuresOS = [];

    this.$opensilex
        .getService(this.scientificObjectsService)
        .searchScientificObjectsWithGeometryListByUris(this.experiment, startDate, endDate)
        .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
              const res = http.response.result as any;
              res.forEach(element => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                    uri: element.uri,
                    name: element.name,
                    type: element.rdf_type,
                    nature: "ScientificObjects",
                    display: "true"
                  };

                  let inserted = false;
                  this.featuresOS.forEach(item => {
                    if (item[0].properties.type == element.rdf_type) {
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
      console.debug("showScientificObjects", scientificObjectUri);
      this.$opensilex
          .getService(this.scientificObjectsService)
          .getScientificObjectDetail(scientificObjectUri, this.experiment)
          .then((http: HttpResponse<OpenSilexResponse<ScientificObjectDetailDTO>>) => {
                let result = http.response.result;
                this.selectedFeatures.forEach(item => {
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
    features.forEach(item => {
      const {uri: uriItem} = item.properties;
      let regExp = /area:.+|area#.+/;

      if (uri.slice(uri.search(regExp) + 5) == uriItem.slice(uriItem.search(regExp) + 5)) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromTemporalAreas(uri, features) {
    features.forEach(item => {
      const uriItem = item.uri;
      let regExp = /events:.+|events#.+/;

      if (uri.slice(uri.search(regExp) + 5) == uriItem.slice(uriItem.search(regExp) + 5)) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromTemporalAreasByTarget(uri, features) {
    features.forEach(item => {
      const uriItem = item.targets[0];
      let regExp = /area:.+|area#.+/;

      if (uri.slice(uri.search(regExp) + 5) == uriItem.slice(uriItem.search(regExp) + 5)) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromFeatureOS(uri, features, higherLevelFeatures = []) {
    features.forEach(item => {
      if (item.type === undefined) {
        this.removeFromFeatureOS(uri, item, features);
      } else {
        const {uri: uriItem} = item.properties;

        if (uri == uriItem) {
          if (features.length > 1 || higherLevelFeatures.length == 0) {
            features.splice(features.indexOf(item), 1);
          } else {
            for (let i = 0; i < higherLevelFeatures.length; i++) {
              if (uri == higherLevelFeatures[i][0].properties.uri) {
                higherLevelFeatures.splice(i, 1);
              }
            }
          }
          return; // Stops at the first matching element
        }
      }
    });
  }

  private deleteEvent(uri) {
    const eventsService = "EventsService";
    this.$opensilex
      .getService(eventsService)
      .searchEvents(undefined, undefined, undefined, uri)
      .then((http: HttpResponse<OpenSilexResponse<EventGetDTO>>) => {
        const res = http.response.result[0] as any;
        this.$opensilex
          .getService(eventsService)
          .deleteEvent(res.uri)
          .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
            let message =
              this.$i18n.t("component.area.title") +
              " " +
              uri +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);
            this.removeFromFeaturesArea(uri, this.featuresArea);
            this.removeFromTemporalAreas(http.response.result, this.temporalAreas);
          })
          .catch(this.$opensilex.errorHandler);
      })
      .catch(this.$opensilex.errorHandler);
  }

  private extracted(res: Array<ResourceTreeDTO>, typeLabel: { uri: string; name: string }[]) {
    res.forEach(({name, uri, children}) => {
      typeLabel.push({
        uri: uri,
        name: name.substr(0, 1).toUpperCase() + name.substr(1)
      });
      if (children.length > 0) {
        this.extracted(children, typeLabel);
      }
    });
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
      coordinateExtent[0] +
      (coordinateExtent[2] - coordinateExtent[0]) * 0.0303111,
      coordinateExtent[3] +
      (coordinateExtent[1] - coordinateExtent[3]) * 0.025747
    ];
  }

  private calcCenterMap(coordinateExtent) {
    this.centerMap = [
      (coordinateExtent[0] + coordinateExtent[2]) / 2,
      (coordinateExtent[1] + coordinateExtent[3]) / 2
    ];
  }

  private areaRecovery() {
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
            [coordinateExtent[2], coordinateExtent[1]]
          ]
        ]
      };

      this.featuresArea = [];
      this.temporalAreas = [];
      this.$opensilex
          .getService(this.areaService)
          .searchIntersects(JSON.parse(JSON.stringify(geometry)))
          .then((http: HttpResponse<OpenSilexResponse<Array<AreaGetDTO>>>) => {
            const res = http.response.result as any;
            res.forEach(element => {
              if (element.geometry != null) {
                element.geometry.properties = {
                  uri: element.uri,
                  name: element.name,
                  type: element.rdf_type,
                  description: element.description,
                  nature: "Area"
                };
                let bool = true;
                for (let area of this.featuresArea) {
                  if (area.properties.uri == element.geometry.properties.uri) { // Check if already exists
                    bool = false;
                  }
                }
                if (bool) {
                  this.featuresArea.push(element.geometry);
                  if (element.geometry.properties.type == this.temporalAreaType) {
                    this.appendTemporalArea(element.geometry.properties);
                  }
                }
              }
            });
            this.endReceipt = true;
          })
          .catch(this.$opensilex.errorHandler);
    }
  }

  private edit(data) {
    let uri = data.item.properties.uri;

    if (data.item.properties.nature === "Area") {
      this.$opensilex
          .getService(this.areaService)
          .getByURI(uri)
          .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
            let form: any = http.response.result;
            this.areaForm.showEditForm(form);
          })
          .catch(this.$opensilex.errorHandler);
    } else {
      this.callSO = true;
      this.scientificObjectURI = uri;
      this.soForm.editScientificObject(uri);
    }
  }
}
</script>

<style lang="scss" scoped>
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

#PerennialArea {
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
    LegendPerennialArea: Perennial Area
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
    mapPanelAreasPerennialZone: Perennial zone
    mapPanelAreasTemporalZone: Temporal zone
    mapPanelFilters: Filters
    create-filter: Create filter
    center: Refocus the map
    save: Save the map
    time: See temporal(s) zone(s)
    noFilter: No filter applied. To add one, use the form below the map
    save-confirmation: Do you want to export the map as PNG image or PDF ?
    noTemporalAreas: No temporal areas are displayed on the map.
    marketFrom: From
    marketTo: To
  Area:
    title: Area
    add: Description of the area
    update: Update Area
    display: Areas
  Filter:
    add: Creation of the filter
    update: Update Filter
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
    LegendPerennialArea: Zone pérenne
    LegendTemporalArea: Zone temporelle
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
    eventPanel-help: Le panneau des événements affiche tous les événements liés aux zones temporelles. Ils sont affichés des plus récents aux plus anciens.
    mapPanelScientificObjects: Objets Scientifiques
    mapPanelAreas: Zones
    mapPanelAreasPerennialZone: Zone pérenne
    mapPanelAreasTemporalZone: Zone temporelle
    mapPanelFilters: Filtres
    create-filter: Créer un filtre
    center: Recentrer la carte
    save: Enregistrer la carte
    time: Visualiser les zones temporelles
    noFilter: Aucun filtre appliqué. Pour en ajouter, utiliser le formulaire situé sous la carte
    save-confirmation: Voulez-vous exporter la carte au format PNG ou PDF ?
    noTemporalAreas: Aucune zone temporaire n'est affichée sur la carte.
    marketFrom: Du
    marketTo: Jusqu'au
  Area:
    title: Zone
    add: Description de la zone
    update: Mise à jour de la zone
    display: Zones
  Filter:
    add: Création d'un filtre
    update: Mise à jour du filtre
  ScientificObjects:
    title: Objet scientifique
    update: L'objet scientifique a été mis à jour
    display: Objets scientifiques
</i18n>
