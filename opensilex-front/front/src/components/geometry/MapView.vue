<template>
  <div id="map">
    <div v-if="!editingMode" id="selected">
      <opensilex-CreateButton v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
                              label="MapView.add-area-button"
                              @click="editingMode = true"
      ></opensilex-CreateButton>
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
    ></opensilex-ModalForm>

    <p class="alert-info">
      <span v-if="!editingMode" v-html="$t('credential.geometry.instruction')"></span>
    </p>
    <div id="mapPoster" :class="editingMode ? 'bg-light border border-secondary' : ''">
      <vl-map
          :default-controls="mapControls"
          :load-tiles-while-animating="true"
          :load-tiles-while-interacting="true"
          data-projection="EPSG:4326"
          style="height: 400px"
          @created="mapCreated"
      >
        <vl-view ref="mapView" :rotation.sync="rotation"></vl-view>

        <vl-layer-tile id="osm">
          <vl-source-osm></vl-source-osm>
        </vl-layer-tile>

        <template v-if="endReceipt">
          <vl-layer-vector>
            <vl-source-vector
                ref="vectorSource"
                :features.sync="features"
                @update:features="defineCenter">
            </vl-source-vector>
          </vl-layer-vector>
          <vl-layer-vector>
            <vl-source-vector ref="vectorSourceArea" :features.sync="featuresArea"></vl-source-vector>
            <vl-style-box>
              <vl-style-stroke color="green"></vl-style-stroke>
              <vl-style-fill color="rgba(200,255,200,0.4)"></vl-style-fill>
            </vl-style-box>
          </vl-layer-vector>
        </template>

        <template v-on="editingMode">
          <div id="editionMode">
            <vl-layer-vector :visible="false">
              <vl-source-vector
                  :features.sync="temporaryArea"
                  ident="the-source"
                  @update:features="memorizesArea">
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
        />
      </vl-map>
    </div>

    <div id="selectedTable">
      <opensilex-TableView
          v-if="selectedFeatures.length !== 0"
          :fields="fieldsSelected"
          :items="selectedFeatures">
        <template v-slot:cell(name)="{data}">
          <opensilex-UriLink
              :uri="data.item.properties.uri"
              :value="data.item.properties.name"
              :noExternalLink="true"
          ></opensilex-UriLink>
        </template>

        <template v-slot:cell(type)="{data}">
          {{ nameType(data.item.properties.type) }}
        </template>

        <template v-slot:cell(description)="{data}">
          {{ data.item.properties.description }}
        </template>

        <template v-slot:cell(actions)="{data}">
          <b-button-group size="sm">
            <div
                v-if="user.admin === true && (data.item.properties.uri.includes('-area') || data.item.properties.uri.includes('set/area#'))">
              <opensilex-DeleteButton v-if="user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)"
                                      label="MapView.delete-area-button"
                                      @click="selectedFeatures.splice(selectedFeatures.indexOf(data.item),1) && deleteArea(data.item.properties.uri)"
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
import {ScientificObjectNodeDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {transformExtent} from "vuelayers/src/ol-ext/proj";
import {AreaGetSingleDTO} from "opensilex-core/model/areaGetSingleDTO";
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {defaults, ScaleLine} from 'ol/control'
import Oeso from "../../ontologies/Oeso";
import {RDFClassDTO} from "opensilex-core/model/rDFClassDTO";
import * as turf from "@turf/turf";
import MultiPolygon from "ol/geom/MultiPolygon";

@Component
export default class MapView extends Vue {
  @Ref("mapView") readonly mapView!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("areaForm") readonly areaForm!: any;

  $opensilex: any;
  $store: any;
  el: "map";
  service: any;
  features: any[] = [];
  featuresArea: any[] = [];
  temporaryArea: any[] = [];
  fieldsSelected = [
    {
      key: "name",
      label: "MapView.name",
    },
    {
      key: "type",
      label: "type",
    },
    {
      key: "description",
      label: "MapView.description",
    },
    {
      key: "actions",
      label: "actions"
    }
  ];
  selectedFeatures: any[] = [];
  nodes = [];

  private editingMode: boolean = false;
  private endReceipt: boolean = false;
  private errorGeometry: boolean = false;
  private coordinateExtent: any;
  private typeLabel: { uri: String; name: String }[] = [];
  private lang: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  data() {
    return {
      rotation: 0,
      mapControls: defaults().extend([
        new ScaleLine(),
      ]),
    };
  }

  showAreaDetails(areaUriResult: any) {
    areaUriResult.then(areaUri => {
      if (areaUri != undefined) {
        this.editingMode = false;
        console.debug("showAreaDetails", areaUri);
        this.$opensilex.getService("opensilex.AreaService")
            .getByURI(areaUri)
            .then((http: HttpResponse<OpenSilexResponse<AreaGetSingleDTO>>) => {
                  const res = http.response.result as any;
                  if (res.geometry != null) {
                    res.geometry.properties = {
                      uri: res.uri,
                      name: res.name,
                      type: res.type,
                      description: res.description,
                    }
                    this.featuresArea.push(res.geometry)
                  }
                }
            )
            .catch(this.$opensilex.errorHandler);
      }
    });
  }

  memorizesArea() {
    if (this.temporaryArea.length) {
      this.$store.state.zone = this.temporaryArea.pop();

      let feature = this.$store.state.zone;

      if (feature.geometry.type === "Polygon") {
        let kinkedPoly = turf.polygon(feature.geometry.coordinates);
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

      for (let element of this.$store.state.zone.geometry.coordinates[0]) {
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
    this.$store.state.experiment = decodeURIComponent(this.$route.params.uri);
    this.retrievesNameOfType()

    this.service = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
    );
    this.service
        .searchScientificObjectsWithGeometryListByUris(this.$store.state.experiment)
        .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
              const res = http.response.result as any;
              res.forEach((element) => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                    uri: element.uri,
                    name: element.name,
                    type: element.type,
                    description: element.description,
                  }
                  this.features.push(element.geometry)
                }
              });
              if (res.length != 0) {
                this.endReceipt = true;
              }
            }
        )
        .catch(this.$opensilex.errorHandler);
  }

  mapCreated(map) {
    // a DragBox interaction used to select features by drawing boxes
    const dragBox = new DragBox({
      condition: platformModifierKeyOnly,
      onBoxEnd: () => {
        // features that intersect the box are selected
        const extent = dragBox.getGeometry().getExtent();
        const source = (this.$refs.vectorSource as any).$source;

        source.forEachFeatureIntersectingExtent(extent, (feature: any) => {
          feature = olExt.writeGeoJsonFeature(feature);
          this.selectedFeatures.push(feature);
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
    let extent = this.vectorSource.$source.getExtent();
    extent[0] -= 50;
    extent[1] -= 50;
    extent[2] += 50;
    extent[3] += 50;
    this.mapView.$view.fit(extent);
    this.areaRecovery(extent);
  }

  select(value) {
    this.$emit("select", value);
  }

  retrievesNameOfType() {
    let typeLabel: { uri: String; name: String }[] = [];
    this.lang = this.user.locale;

    this.service = this.$opensilex.getService(
        "opensilex.OntologyService"
    );

    this.service.getSubClassesOf(Oeso.SCIENTIFIC_OBJECT_TYPE_URI, true)
        .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
              const res = http.response.result;
              res.forEach(({name, uri}) => {
                typeLabel.push({uri: uri, name: name});
              });
            }
        )
        .catch(this.$opensilex.errorHandler);

    this.service.getClass(Oeso.AREA)
        .then((http: HttpResponse<OpenSilexResponse<RDFClassDTO>>) => {
              const res = http.response.result;
              typeLabel.push({uri: res.uri, name: res.label});
            }
        )
        .catch(this.$opensilex.errorHandler);

    this.service.getSubClassesOf(Oeso.PERENNIAL_AREA, true)
        .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
              const res = http.response.result;
              res.forEach(({name, uri}) => {
                typeLabel.push({uri: uri, name: name});
              });
            }
        )
        .catch(this.$opensilex.errorHandler);

    this.typeLabel = typeLabel;
  }

  nameType(uriType) {
    if (this.user.locale != this.lang) {
      this.retrievesNameOfType();
    }

    for (let typeLabelElement of this.typeLabel) {
      if (typeLabelElement.uri == uriType)
        return typeLabelElement.name;
    }
  }

  private areaRecovery(extent) {
    extent[0] -= 450;
    extent[1] -= 100;
    extent[2] += 450;
    extent[3] += 100;

    this.coordinateExtent = transformExtent(extent, "EPSG:3857", "EPSG:4326");

    let geometry = {
      "type": "Polygon",
      "coordinates": [[
        [this.coordinateExtent[2], this.coordinateExtent[1]],
        [this.coordinateExtent[0], this.coordinateExtent[1]],
        [this.coordinateExtent[0], this.coordinateExtent[3]],
        [this.coordinateExtent[2], this.coordinateExtent[3]],
        [this.coordinateExtent[2], this.coordinateExtent[1]]
      ]]
    }
    this.service = this.$opensilex.getService(
        "opensilex.AreaService"
    );
    this.service
        .searchIntersects(JSON.parse(JSON.stringify(geometry)))
        .then((http: HttpResponse<OpenSilexResponse<Array<AreaGetSingleDTO>>>) => {
              const res = http.response.result as any;
              res.forEach((element) => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                    uri: element.uri,
                    name: element.name,
                    type: element.type,
                    description: element.description,
                  }
                  this.featuresArea.push(element.geometry)
                }
              });
            }
        )
        .catch(this.$opensilex.errorHandler);
  }

  private deleteArea(uri) {
    this.$opensilex.getService("opensilex.AreaService")
        .deleteArea(uri)
        .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
          let message =
              this.$i18n.t("Area.title") +
              " " +
              http.response.result +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
          this.$opensilex.showSuccessToast(message);

          for (let featuresAreaElement of this.featuresArea) {
            if (featuresAreaElement.properties.uri == uri) {
              this.featuresArea.splice(this.featuresArea.indexOf(featuresAreaElement), 1)
            }
          }
        })
        .catch(this.$opensilex.errorHandler);
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
</style>

<i18n>
en:
  MapView:
    name: name
    description: description
    label: Geometry
    add-button: Add metadata
    add-area-button: Add area
    delete-area-button: Delete an area
    selected-button: Exit creation mode
    errorLongitude: the longitude must be between -180 and 180
    errorLatitude: the latitude must be between -90 and 90
  Area:
    title: Area
    add: Description of the area
    update: Update a perennial zone
fr:
  MapView:
    name: nom
    description: description
    label: Géométrie
    add-button: Ajouter des métadonnées
    add-area-button: Ajouter une zone
    delete-area-button: Supprimer une zone
    selected-button: Sortir du mode création
    errorLongitude: la longitude doit être comprise entre -180 et 180
    errorLatitude: la latitude doit être comprise entre -90 et 90
  Area:
    title: Zone
    add: Description de la zone
    update: Mettre à jour une zone pérenne

</i18n>