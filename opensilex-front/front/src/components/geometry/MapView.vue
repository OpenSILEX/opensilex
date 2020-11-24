<template>
  <div id="map">
    <div>
      <h1>{{ nameExperiment }}</h1>
    </div>
    <div id="editing" class="row" v-if="!editingMode">
      <opensilex-CreateButton v-if="user.hasCredential(credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID)"
                              label="MapView.add-area-button"
                              @click="editingMode = true"
      ></opensilex-CreateButton>
      <div v-if="selectedFeatures.length === 1 && selectedFeatures[0].properties.uri.includes('-area')">
        <opensilex-DeleteButton v-if="user.hasCredential(credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID)"
                                label="MapView.add-area-button"
                                @click="deleteArea(selectedFeatures[0].properties.uri)"
        ></opensilex-DeleteButton>
      </div>
    </div>
    <opensilex-InteroperabilityButton v-if="editingMode"
                                      label="MapView.selected-button"
                                      @click="editingMode = false"
    ></opensilex-InteroperabilityButton>
    <!--    <div v-if="editingAreaPopUp && editingMode">-->
    <!--      {{ this.$bvModal.show("eventArea") }}-->
    <!--      <b-modal-->
    <!--          :title="$t('component.area.eventArea')"-->
    <!--          centered-->
    <!--          hide-footer-->
    <!--          id="eventArea"-->
    <!--      >-->
    <!--        <opensilex-CreateButton-->
    <!--            @click="(areaForm.showCreateForm())"-->
    <!--            label="component.area.add-button"-->
    <!--            v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"-->
    <!--        ></opensilex-CreateButton>-->
    <!--        <b-button-->
    <!--            @click="editingAreaPopUp = false"-->
    <!--            variant="warning"-->
    <!--        >{{ $t('component.area.no') }}-->
    <!--        </b-button>-->
    <!--      </b-modal>-->
    <!--    </div>-->
    <div v-if="editingArea && editingMode">
      <opensilex-PageActions>
        <template v-slot>
          <opensilex-CreateButton v-if="user.hasCredential(credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID)"
                                  :disabled="!editingArea"
                                  label="MapView.add-button"
                                  @click="areaForm.showCreateForm()"
          ></opensilex-CreateButton>
        </template>
      </opensilex-PageActions>
    </div>
    <opensilex-ModalForm
        ref="areaForm"
        :successMessage="successMessageArea"
        component="opensilex-AreaForm"
        createTitle="Area.add"
        editTitle="Area.update"
        icon="fa#sun"
        modalSize="xl"
        @onCreate="showAreaDetails"
    ></opensilex-ModalForm>

    <div id="selectionMode">
      {{ $t('credential.geometry.instruction') }}
      <vl-map
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

        <template v-if="endReceipt && !editingMode">
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

        <template v-if="editingMode">
          <div id="editionMode">
            <vl-layer-vector>
              <vl-source-vector
                  :features.sync="temporaryArea"
                  ident="the-source"
                  @update:features="memorizesArea">
              </vl-source-vector>
            </vl-layer-vector>
            <vl-layer-vector>
              <vl-source-vector :features.sync="features"></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="#ff3620"></vl-style-stroke>
                <vl-style-fill color="rgba(255,255,255,0.5)"></vl-style-fill>
              </vl-style-box>
            </vl-layer-vector>
            <vl-layer-vector>
              <vl-source-vector ref="vectorSourceArea" :features.sync="featuresArea"></vl-source-vector>
              <vl-style-box>
                <vl-style-stroke color="green"></vl-style-stroke>
                <vl-style-fill color="rgba(200,255,200,0.4)"></vl-style-fill>
              </vl-style-box>
            </vl-layer-vector>

            <!-- Creating a new area -->
            <vl-interaction-draw
                :type=drawType
                source="the-source"
                @drawend="(editingArea = true) && (editingAreaPopUp = true) && (areaForm.showCreateForm())"
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
            id="select"
            ref="selectInteraction"
            v-if="!editingMode"
            :features.sync="selectedFeatures"
        />
      </vl-map>

      <div id="selectedTable">
        <b-table
            v-if="selectedFeatures.length !== 0"
            :fields="fieldsSelected"
            :items="selectedFeatures"
            hover
            sort-icon-left
            striped
        >
          <template v-slot:cell(name)="data">
            <opensilex-UriLink
                :uri="data.item.properties.uri"
                :value="data.item.properties.name"
                :to="{path: encodeURIComponent(data.item.properties.uri)}"
            ></opensilex-UriLink>
          </template>
        </b-table>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {DragBox} from "ol/interaction";
import {platformModifierKeyOnly} from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
import {ExperimentGetDTO, ScientificObjectNodeDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {transformExtent} from "vuelayers/src/ol-ext/proj";
import {AreaGetSingleDTO} from "opensilex-core/model/areaGetSingleDTO";
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";

@Component
export default class MapView extends Vue {
  @Ref("mapView") readonly mapView!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  @Ref("modalRef") readonly modalRef!: any;
  @Ref("geometryForm") readonly geometryForm!: any;
  @Ref("areaForm") readonly areaForm!: any;

  $opensilex: any;
  $store: any;
  el: "map";
  service: any;
  features: any[] = [];
  featuresArea: any[] = [];
  temporaryArea: any[] = [];
  arrayPrefix: any[] = [];
  fieldsSelected = [
    {
      key: "name",
      label: "name",
      sortable: true
    },
    {
      key: "properties.type",
      label: "type",
      sortable: true
    },
    {
      key: "properties.comment",
      label: "comment",
      sortable: true
    }
  ];
  selectedFeatures: any[] = [];
  nodes = [];

  private nameExperiment: string = "";
  private drawType: string = "Polygon";
  private editingMode: boolean = false;
  private editingArea: boolean = false;
  private editingAreaPopUp: boolean = false;
  private endReceipt: boolean = false;
  // private drawControls = [];
  private coordinateExtent: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  data() {
    return {
      rotation: 0,
    };
  }

  showAreaDetails(areaUriResult: any) {
    this.editingMode = false;
    areaUriResult.then(areaUri => {
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
                    comment: res.comment,
                  }
                  this.featuresArea.push(res.geometry)
                }
              }
          )
          .catch(this.$opensilex.errorHandler);
    });
  }

  memorizesArea() {
    this.$store.state.zone = this.temporaryArea.pop();
  }

  // deleteLastFieldNotValidated() {
  //   this.temporaryArea.splice(this.temporaryArea.length - 1);
  // }

  showCreateForm() {
    this.areaForm.showCreateForm();
  }

  created() {
    this.$store.state.experiment = decodeURIComponent(this.$route.params.uri);
    this.loadNameExperiment();
    // this.loadDrawTypes();
    // this.loadNamespaces();

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
                    comment: element.comment,
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

  // loadDrawTypes() {
  //   this.drawControls = [
  //     {
  //       id: 'point',
  //       label: this.$i18n.t("Area.point")
  //     },
  //     {
  //       id: 'line-string',
  //       label: this.$i18n.t("Area.line-string")
  //     },
  //     {
  //       id: 'polygon',
  //       label: this.$i18n.t("Area.polygon")
  //     },
  //     {
  //       id: 'circle',
  //       label: this.$i18n.t("Area.circle")
  //     },
  //     {
  //       id: 'undefined',
  //       label: this.$i18n.t("Area.stop")
  //     }
  //   ];
  // }

  successMessageArea() {
    // this.$bvModal.hide("eventAnnotation");
    // this.editingAreaPopUp = false;
    this.editingArea = false;

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

  loadNameExperiment() {
    let service = this.$opensilex.getService(
        "opensilex.ExperimentsService"
    );

    service.getExperiment(this.$store.state.experiment)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          this.nameExperiment = http.response.result.label;
        })
        .catch((error) => {
          this.$opensilex.errorHandler(error);
        });
  }

  select(value) {
    this.$emit("select", value);
  }

  private areaRecovery(extent) {
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
                    comment: element.comment,
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

  // getByUriScientificObject() {
  //   let objectURI = []
  //   this.selectedFeatures.forEach(item => {
  //     objectURI.push(item.properties.uri)
  //   });
  //
  //   return this.$opensilex
  //       .getService("opensilex.ScientificObjectsService")
  //       .getScientificObjectsListByUris(
  //           this.$store.state.experiment,
  //           objectURI
  //       );
  // }
}
</script>

<i18n>
    en:
      MapView:
        label: Geometry
        add-button: Input annotation
        add-area-button: Area
        selected-button: Return to selection mode
        add: Create metadata
        update: Update metadata
        uri: Geometry URI
      Area:
        title: Area
        editing: Yes
        selection: No
        choiceTypeGeometriesDrawn: Choice type geometries to be drawn
        add: Description of the area
        update: Update a perennial zone
        point: Point
        line-string: LineString
        polygon: Polygon
        circle: Circle
        stop: Stop drawing
    fr:
      MapView:
        label: Géométrie
        add-button: Zone
        add-area-button: Zone
        selected-button: Retour au mode de sélection
        add: Créer une annotation
        update: Mettre à jour annotation
        uri: URI de Géométrie
      Area:
        title: Zone
        editing: Oui
        selection: Non
        choiceTypeGeometriesDrawn: Choix du type de géométrie à dessiner
        add: Description de la zone
        update: Mettre à jour une zone pérenne
        point: Point
        line-string: LineString
        polygon: Polygone
        circle: Cercle
        stop: Arrêter de dessiner
</i18n>