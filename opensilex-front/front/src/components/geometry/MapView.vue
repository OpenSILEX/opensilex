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
        <b-button v-b-toggle.map-sidebar>
          {{ $t("MapView.configuration") }}
        </b-button>
        <!--// map panel, controls -->
      </div>
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
    <div v-if="editingMode" id="editing">
      <opensilex-Button
          :small="false"
          icon=""
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
        modalSize="xl"
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

    <div
        id="mapPoster"
        :class="editingMode ? 'bg-light border border-secondary' : ''"
    >
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
        >
        </vl-view>

        <vl-layer-tile id="osm">
          <vl-source-osm :wrap-x="false"/>
        </vl-layer-tile>

        <vl-overlay
            v-if="selectPointerMove.length !== 0"
            id="overlay"
            :position="overlayCoordinate"
        >
          <template slot-scope="scope">
            <div class="panel-content">
              {{ selectPointerMove[0] + " (" + selectPointerMove[1] + ")" }}
            </div>
          </template>
        </vl-overlay>

        <vl-overlay
            v-if="selectedFeatures.length === 1"
            id="detailItem"
            :position="centerMap"
            positioning="center-center"
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
          <vl-layer-vector :visible="displayAreas === 'true'">
            <vl-source-vector
                ref="vectorSourceArea"
                :features.sync="featuresArea"
            ></vl-source-vector>
            <vl-style-box>
              <vl-style-stroke color="green"></vl-style-stroke>
              <vl-style-fill color="rgba(200,255,200,0.4)"></vl-style-fill>
            </vl-style-box>
          </vl-layer-vector>
          <vl-layer-vector
              v-for="layerSO in featuresOS"
              :key="layerSO.id"
              :visible="displaySO === 'true' && layerSO[0].properties.display==='true'"
          >
            <vl-source-vector
                ref="vectorSource"
                :features="layerSO"
                @mounted="defineCenter"
            >
            </vl-source-vector>
          </vl-layer-vector>
          <vl-layer-vector
              v-for="layer in tabLayer"
              :key="layer.ref"
              :visible="layer.display === 'true'"
          >
            <vl-source-vector
                :ref="layer.ref"
                :features.sync="layer.tabFeatures"
            ></vl-source-vector>
            <vl-style-box>
              <vl-style-stroke
                  v-if="layer.vlStyleStrokeColor"
                  :color="layer.vlStyleStrokeColor"
              ></vl-style-stroke>
              <!-- outline color -->
              <vl-style-fill
                  v-if="layer.vlStyleFillColor"
                  :color="colorFeature(layer.vlStyleFillColor)"
              ></vl-style-fill>
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
              >
              </vl-source-vector>
            </vl-layer-vector>

            <!-- Creating a new area -->
            <vl-interaction-draw
                v-if="editingMode"
                source="the-source"
                type="Polygon"
                @drawend="areaForm.showCreateForm()"
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
            :features.sync="selectedFeatures"
            @update:features=" selectedFeatures.length === 1 ? showDetails(selectedFeatures[0], true) : ''"
        />
      </vl-map>
    </div>

    <b-sidebar
        id="map-sidebar"
        :title="$t('MapView.configuration')"
        class="sidebar-content"
    >
      <h5 class="text">{{ $t("MapView.display") + " : " }}</h5>
      <br/>
      <ul class="list-group">
        <li class="list-group-item">
          <opensilex-CheckboxForm
              :value.sync="displaySO"
              class="col-lg-2"
              title="ScientificObjects.display"
              @update:value="displayScientificObjects"
          ></opensilex-CheckboxForm>
        </li>
        <li
            v-for="layerSO in featuresOS"
            :key="layerSO.id"
            class="list-group-item d-flex justify-content-around"
        >
          <opensilex-CheckboxForm
              :title="nameType(layerSO[0].properties.type)"
              :value.sync="layerSO[0].properties.display"
              class="p-2 bd-highlight"
          ></opensilex-CheckboxForm>
        </li>
        <li class="list-group-item">
          <opensilex-CheckboxForm
              :value.sync="displayAreas"
              class="p2"
              title="Area.display"
          ></opensilex-CheckboxForm>
        </li>
        <li
            v-for="layer in tabLayer"
            :key="layer.ref"
            class="list-group-item d-flex justify-content-around"
        >
          <opensilex-CheckboxForm
              :title="layer.titleDisplay"
              :value.sync="layer.display"
              class="p-2 bd-highlight"
          ></opensilex-CheckboxForm>
          <div class="p-2 bd-highlight col-2">
            <opensilex-InputForm
                v-if="layer.vlStyleStrokeColor"
                :value.sync="layer.vlStyleStrokeColor"
                type="color"
            ></opensilex-InputForm>
            <opensilex-InputForm
                v-if="layer.vlStyleFillColor"
                :value.sync="layer.vlStyleFillColor"
                type="color"
            ></opensilex-InputForm>
          </div>
          <opensilex-DeleteButton
              label="FilterMap.filter.delete-button"
              @click="tabLayer.splice(tabLayer.indexOf(layer), 1)"
          ></opensilex-DeleteButton>
        </li>
      </ul>
    </b-sidebar>

    {{ $t("MapView.Legend") }}:
    <span id="OS">{{ $t("MapView.LegendSO") }}</span>
    &nbsp;-&nbsp;
    <span id="Area">{{ $t("MapView.LegendArea") }}</span>
    <opensilex-FilterMap
        :experiment="experiment"
        :featureOS="featuresOS"
        :tabLayer="tabLayer"
    >
    </opensilex-FilterMap>
    <div id="selectedTable">
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

        <template v-slot:cell(type)="{ data }">
          {{ nameType(data.item.properties.type) }}
        </template>

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
import {platformModifierKeyOnly} from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
// @ts-ignore
import {ScientificObjectNodeDTO} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {transformExtent} from "vuelayers/src/ol-ext/proj";
// @ts-ignore
import {AreaGetDTO} from "opensilex-core/model/areaGetDTO";
// @ts-ignore
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
// @ts-ignore
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {defaults, ScaleLine} from "ol/control";
import Oeso from "../../ontologies/Oeso";
import * as turf from "@turf/turf";
// @ts-ignore
import {ScientificObjectDetailDTO} from "opensilex-core/model/scientificObjectDetailDTO";

@Component
export default class MapView extends Vue {
  @Ref("mapView") readonly mapView!: any;
  @Ref("map") readonly map!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("areaForm") readonly areaForm!: any;
  @Ref("soForm") readonly soForm!: any;

  $opensilex: any;
  $t: any;
  $store: any;
  el: "map";
  service: any;
  featuresOS: any[] = [];
  featuresArea: any[] = [];
  temporaryArea: any[] = [];
  selectPointerMove: any[] = [];
  overlayCoordinate: any[] = [];
  centerMap: any[] = [];
  tabLayer: any[] = [];
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
  nodes: any[] = [];

  private editingMode: boolean = false;
  private displayAreas: String = "true";
  private displaySO: String = "true";
  private subDisplaySO: string[] = [];
  private detailsSO: boolean = false;
  private endReceipt: boolean = false;
  private errorGeometry: boolean = false;
  private callSO: boolean = false;
  private typeLabel: { uri: String; name: String }[] = [];
  private lang: string;
  private mapControls = defaults().extend([new ScaleLine()]);
  private experiment: string;
  private scientificObjectsService = "opensilex.ScientificObjectsService";
  private areaService = "opensilex.AreaService";
  private scientificObjectURI: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  displayScientificObjects() {
    if (this.displaySO == "false") {
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
      areaUriResult.then((areaUri) => {
        this.recoveryShowArea(areaUri);
      });
    } else {
      this.recoveryShowArea(areaUriResult);
    }
  }

  onMapPointerMove({pixel}: any) {
    // Gets the name when the cursor hovers over the item.
    const hitFeature = this.map.forEachFeatureAtPixel(
        pixel,
        (feature) => feature
    );
    if (hitFeature) {
      this.selectPointerMove = [
        hitFeature.values_.name,
        this.nameType(hitFeature.values_.type),
      ];
    } else {
      this.selectPointerMove = [];
    }
  }

  callAreaUpdate(areaUriResult) {
    if (areaUriResult instanceof Promise) {
      areaUriResult.then((areaUri) => {
        this.recoveryArea(areaUri);
      });
    } else {
      this.recoveryArea(areaUriResult);
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
                    display: "true",
                  };

                  let inserted = false;
                  this.featuresOS.forEach((item) => {
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

          unKinkedPoly.features.forEach((item) => {
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

  created() {
    this.$opensilex.showLoader();

    this.experiment = decodeURIComponent(this.$route.params.uri);
    this.retrievesNameOfType();
    this.recoveryScientificObjects();
  }

  mapCreated(map) {
    // a DragBox interaction used to select features by drawing boxes
    const dragBox = new DragBox({
      condition: platformModifierKeyOnly,
      onBoxEnd: () => {
        // features that intersect the box are selected
        const extent = dragBox.getGeometry().getExtent();
        (this.$refs.vectorSource as any).forEach((vector) => {
          const source = vector.$source;

          source.forEachFeatureIntersectingExtent(extent, (feature: any) => {
            feature = olExt.writeGeoJsonFeature(feature);
            this.selectedFeatures.push(feature);
          });
        });
      },
    });

    map.$map.addInteraction(dragBox);

    // clear selection when drawing a new box and when clicking on the map
    dragBox.on("boxStart", () => {
      this.selectedFeatures = [];
    });
  }

  successMessageArea() {
    return this.$i18n.t("MapView.label");
  }

  defineCenter() {
    if (this.featuresOS.length > 0) {
      let extent = [-50, -50, 50, 50];
      this.vectorSource.forEach((vector) => {
        let extentTemporary = vector.$source.getExtent();
        if (extentTemporary[0] != Infinity) {
          extent[0] += extentTemporary[0] / this.vectorSource.length;
          extent[1] += extentTemporary[1] / this.vectorSource.length;
          extent[2] += extentTemporary[2] / this.vectorSource.length;
          extent[3] += extentTemporary[3] / this.vectorSource.length;
        }
      });
      this.mapView.$view.fit(extent);
    }
  }

  select(value) {
    this.$emit("select", value);
  }

  retrievesNameOfType() {
    let typeLabel: { uri: String; name: String }[] = [];
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

  nameType(uriType) {
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
    return "rgba(" + parseInt(color.slice(1, 3), 16) + "," + parseInt(color.slice(3, 5), 16) + "," + parseInt(color.slice(5, 7), 16) + ",0.5)"
  }

  private recoveryShowArea(areaUri) {
    if (areaUri != undefined) {
      this.editingMode = false;
      console.debug("showAreaDetails", areaUri);
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
                nature: "Area",
              };
              this.featuresArea.push(res.geometry);
            }
          })
          .catch(this.$opensilex.errorHandler);
    }
  }

  private recoveryArea(areaUri) {
    if (areaUri != undefined) {
      this.removeFromFeaturesArea(areaUri, this.featuresArea);
      this.removeFromFeaturesArea(areaUri, this.selectedFeatures);

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
                nature: "Area",
              };
              this.featuresArea.push(res.geometry);
              this.selectedFeatures.push(res.geometry);
            }
          })
          .catch(this.$opensilex.errorHandler);
    }
  }

  private recoveryScientificObjects() {
    this.callSO = false;
    this.featuresOS = [];

    this.$opensilex
        .getService(this.scientificObjectsService)
        .searchScientificObjectsWithGeometryListByUris(this.experiment)
        .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
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
        .catch(this.$opensilex.errorHandler)
        .finally(() => {
          this.$opensilex.hideLoader();
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
      const {uri: uriItem} = item.properties;
      let regExp = /area:.+|area#.+/;

      if (uri.slice(uri.search(regExp) + 5) == uriItem.slice(uriItem.search(regExp) + 5)) {
        features.splice(features.indexOf(item), 1);
      }
    });
  }

  private removeFromFeatureOS(uri, features, higherLevelFeatures = []) {
    features.forEach((item) => {
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

  private extracted(res: Array<ResourceTreeDTO>, typeLabel: { uri: String; name: String }[]) {
    res.forEach(({name, uri, children}) => {
      typeLabel.push({
        uri: uri,
        name: name.substr(0, 1).toUpperCase() + name.substr(1),
      });
      if (children.length > 0) {
        this.extracted(children, typeLabel);
      }
    });
  }

  private areaRecovery() {
    let coordinateExtent = transformExtent(
        this.mapView.$view.calculateExtent(),
        "EPSG:3857",
        "EPSG:4326"
    );
    this.overlayCoordinate = [
      coordinateExtent[0] +
      (coordinateExtent[2] - coordinateExtent[0]) * 0.0303111,
      coordinateExtent[3] +
      (coordinateExtent[1] - coordinateExtent[3]) * 0.025747,
    ];

    this.centerMap = [
      (coordinateExtent[0] + coordinateExtent[2]) / 2,
      (coordinateExtent[1] + coordinateExtent[3]) / 2,
    ];

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

      this.featuresArea = [];
      this.$opensilex
          .getService(this.areaService)
          .searchIntersects(JSON.parse(JSON.stringify(geometry)))
          .then((http: HttpResponse<OpenSilexResponse<Array<AreaGetDTO>>>) => {
            const res = http.response.result as any;
            res.forEach((element) => {
              if (element.geometry != null) {
                element.geometry.properties = {
                  uri: element.uri,
                  name: element.name,
                  type: element.rdf_type,
                  description: element.description,
                  nature: "Area",
                };
                this.featuresArea.push(element.geometry);
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

#Area {
  color: green;
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
  width: 240px;
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
    LegendArea: Area
    Instruction: Press Shift to <b>select item by item</b> on the map. Press and hold Shift + Alt + Click and move the mouse to rotate the map. Press Ctrl + Click while dragging to <b>select multiple scientific objects</b>.
    WarningInstruction: Currently, the selection tool does not follow the rotation.
    details: Show or hide element details
    author: Author
    update: Update element
    display: Display of layers
    configuration: Control panel
  Area:
    title: Area
    add: Description of the area
    update: Update Area
    display: Areas
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
    LegendArea: Zone
    Instruction: Appuyez sur Maj pour <b>sélectionner élément par élément</b> sur la carte. Appuyez et maintenez Maj +Alt + Clic puis déplacer la souris pour faire <b>pivoter</b> la carte. Appuyez sur Ctrl + Clic tout en faisant glisser pour <b>sélectionner plusieurs objets scientifiques</b>.
    WarningInstruction: Actuellement, l'outil de sélection ne suit pas la rotation.
    details: Afficher ou masquer les détails de l'élément
    author: Auteur
    update: Mise à jour de l'élément
    display: Affichage des couches
    configuration: Panneau de contrôle
  Area:
    title: Zone
    add: Description de la zone
    update: Mise à jour de la zone
    display: Zones
  ScientificObjects:
    title: Objet scientifique
    update: L'objet scientifique a été mis à jour
    display: Objets scientifiques
</i18n>
