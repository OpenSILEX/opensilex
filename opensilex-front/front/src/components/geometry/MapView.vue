<template>
  <div id="map">
    <div v-if="!editingMode" id="selected">
      <opensilex-CreateButton
          v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
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
        @onUpdate="callAreaUpdate"
    ></opensilex-ModalForm>

    <p class="alert-info">
      <span v-if="!editingMode" v-html="$t('MapView.Instruction')"></span>
    </p>
    <div
        id="mapPoster"
        :class="editingMode ? 'bg-light border border-secondary' : ''"
    >
      <opensilex-CheckboxForm
          v-if="!editingMode"
          :value.sync="displayAreas"
          title="Area.displayAreas"
      ></opensilex-CheckboxForm>

      <!-- "mapControls" to display the scale -->
      <vl-map
          ref="map"
          :default-controls="mapControls"
          :load-tiles-while-animating="true"
          :load-tiles-while-interacting="true"
          data-projection="EPSG:4326"
          style="height: 400px"
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
          <vl-layer-vector>
            <vl-source-vector
                ref="vectorSource"
                :features.sync="featuresOS"
                @update:features="defineCenter"
            >
            </vl-source-vector>
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
        />
      </vl-map>
    </div>
    {{ $t("MapView.Legend") }}:
    <span id="OS">{{ $t("MapView.LegendSO") }}</span>
    &nbsp;-&nbsp;
    <span id="Area">{{ $t("MapView.LegendArea") }}</span>

    <div id="selectedTable">
      <opensilex-TableView
          v-if="selectedFeatures.length !== 0"
          :fields="fieldsSelected"
          :items="selectedFeatures"
      >
        <template v-slot:cell(name)="{ data }">
          <opensilex-UriLink
              :to="data.item.properties.nature === 'Area' ? {path: '/area/details/'+ encodeURIComponent(data.item.properties.uri)} : {}"
              :uri="data.item.properties.uri"
              :value="data.item.properties.name"
          ></opensilex-UriLink>
        </template>

        <template v-slot:cell(type)="{ data }">
          {{ nameType(data.item.properties.type) }}
        </template>

        <template v-slot:row-details="{ data }">
          <div v-if="data.item.properties.nature === 'Area'">
            <strong class="capitalize-first-letter">
              {{ $t("MapView.author") }}
            </strong>
            <br/>
            {{ data.item.properties.author }}
            <br/>
            <strong class="capitalize-first-letter">
              {{ $t("MapView.description") }}
            </strong>
            <br/>
            {{ data.item.properties.description }}
            <br/>
            <opensilex-GeometryView
                v-if="data.item.geometry"
                :value="data.item.geometry"
                label="component.common.geometry"
            ></opensilex-GeometryView>
          </div>
        </template>

        <template v-slot:cell(actions)="{ data }">
          <b-button-group size="sm">
            <div v-if="user.admin === true &&
                 (data.item.properties.uri.includes('-area') || data.item.properties.uri.includes('set/area#'))">
              <opensilex-DetailButton
                  :detailVisible="data['detailsShowing']"
                  :small="true"
                  label="MapView.details"
                  @click="showDetails(data)"
              ></opensilex-DetailButton>

              <opensilex-EditButton v-if=" user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
                                    :small="true"
                                    label="Area.update"
                                    @click="edit(data)"
              ></opensilex-EditButton>

              <opensilex-DeleteButton
                  v-if="user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)"
                  label="MapView.delete-area-button"
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
import {ScientificObjectNodeDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {transformExtent} from "vuelayers/src/ol-ext/proj";
import {AreaGetDTO} from "opensilex-core/model/areaGetDTO";
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {defaults, ScaleLine} from "ol/control";
import Oeso from "../../ontologies/Oeso";
import * as turf from "@turf/turf";
import MultiPolygon from "ol/geom/MultiPolygon";

@Component
export default class MapView extends Vue {
  @Ref("mapView") readonly mapView!: any;
  @Ref("map") readonly map!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("areaForm") readonly areaForm!: any;

  $opensilex: any;
  $store: any;
  el: "map";
  service: any;
  featuresOS: any[] = [];
  featuresArea: any[] = [];
  temporaryArea: any[] = [];
  selectPointerMove: any[] = [];
  overlayCoordinate: any[] = [];
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
  nodes = [];

  private editingMode: boolean = false;
  private displayAreas: String = "true";
  private endReceipt: boolean = false;
  private errorGeometry: boolean = false;
  private typeLabel: { uri: String; name: String }[] = [];
  private lang: string;
  private mapControls = defaults().extend([new ScaleLine()]);

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  showDetails(data) {
    if (!data.detailsShowing) {
      let uriResult = data.item.properties.uri;
      if (data.item.properties.nature === "Area") {
        this.areaDetails(uriResult);
      }
    }
    data.toggleDetails = -data.toggleDetails();
  }

  showAreaDetails(areaUriResult: any) {
    areaUriResult.then((areaUri) => {
      if (areaUri != undefined) {
        this.editingMode = false;
        console.debug("showAreaDetails", areaUri);
        this.$opensilex
            .getService("opensilex.AreaService")
            .getByURI(areaUri)
            .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
              const res = http.response.result as any;
              if (res.geometry != null) {
                res.geometry.properties = {
                  uri: res.uri,
                  name: res.name,
                  type: res.rdf_type,
                  description: res.description,
                  nature: "Area",
                };
                this.featuresArea.push(res.geometry);
              }
            })
            .catch(this.$opensilex.errorHandler);
      }
    });
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
    areaUriResult.then((areaUri) => {
      if (areaUri != undefined) {
        this.removeFromFeaturesArea(areaUri, this.featuresArea);

        this.$opensilex
            .getService("opensilex.AreaService")
            .getByURI(areaUri)
            .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
              const res = http.response.result as any;
              if (res.geometry != null) {
                res.geometry.properties = {
                  uri: res.uri,
                  name: res.name,
                  type: res.rdf_type,
                  description: res.description,
                  nature: "Area",
                };
                this.featuresArea.push(res.geometry);
              }
            })
            .catch(this.$opensilex.errorHandler);
      }
    });
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

    let experiment = decodeURIComponent(this.$route.params.uri);
    this.retrievesNameOfType();

    this.service = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
    );
    this.service
        .searchScientificObjectsWithGeometryListByUris(experiment)
        .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
              const res = http.response.result as any;
              res.forEach((element) => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                    uri: element.uri,
                    name: element.name,
                    type: element.type,
                    description: element.description,
                    nature: "ScientificObjects",
                  };
                  this.featuresOS.push(element.geometry);
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

  private areaDetails(areaUri) {
    console.debug("areaDetails", areaUri);
    this.$opensilex
        .getService("opensilex.AreaService")
        .getByURI(areaUri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          let result = http.response.result;
          this.selectedFeatures.forEach((item) => {
            if (item.properties.uri === result.uri) {
              item.properties.description = result.description;
              item.properties.author = result.author;
            }
          });
        })
        .catch(this.$opensilex.errorHandler);
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
      this.service = this.$opensilex.getService("opensilex.AreaService");
      this.service
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
    } else {
      return;
    }
  }

  private edit(data) {
    let uri = data.item.properties.uri;

    if (data.item.properties.nature === "Area") {
      this.removeFromFeaturesArea(uri, this.selectedFeatures);
      this.$opensilex
          .getService("opensilex.AreaService")
          .getByURI(uri)
          .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
            let form: any = http.response.result;
            this.areaForm.showEditForm(form);
          })
          .catch(this.$opensilex.errorHandler);
    }
  }

  private deleteItem(data) {
    let uri = data.item.properties.uri;

    if (data.item.properties.nature === "Area") {
      this.$opensilex
          .getService("opensilex.AreaService")
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
    delete-area-button: Delete area
    selected-button: Exit creation mode
    errorLongitude: the longitude must be between -180 and 180
    errorLatitude: the latitude must be between -90 and 90
    Legend: Legend
    LegendSO: Scientific Object
    LegendArea: Area
    Instruction: Press Shift to <b>select item by item</b> on the map. Press and hold Shift + Alt + Click and move the mouse to rotate the map. Press Ctrl + Click while dragging to <b>select multiple scientific objects</b>.
    details: Show or hide element details
    author: Author
  Area:
    title: Area
    add: Description of the area
    update: Update Area
    displayAreas: Display of areas
fr:
  MapView:
    name: nom
    description: description
    type: type
    label: Géométrie
    add-button: Ajouter des métadonnées
    add-area-button: Ajouter une zone
    delete-area-button: Supprimer la zone
    selected-button: Sortir du mode création
    errorLongitude: la longitude doit être comprise entre -180 et 180
    errorLatitude: la latitude doit être comprise entre -90 et 90
    Legend: Légende
    LegendSO: Objet scientifique
    LegendArea: Zone
    Instruction: Appuyez sur Shift pour <b>sélectionner élément par élément</b> sur la carte. Appuyez et maintenez Shift +Alt + Clic puis déplacer la souris pour faire <b>pivoter</b> la carte. Appuyez sur Ctrl + Clic tout en faisant glisser pour <b>sélectionner plusieurs objets scientifiques</b>.
    details: Afficher ou masquer les détails de l'élément
    author: Auteur
  Area:
    title: Zone
    add: Description de la zone
    update: Mise à jour de la zone
    displayAreas: Affichage des zones
</i18n>