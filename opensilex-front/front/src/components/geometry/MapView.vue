<template>
  <div id="map">
    <div>
      <h1>{{ nameExperiment }}</h1>
    </div>
    <div id="editing" class="row">
      <h6>{{ $t("MapView.add") }}</h6>
      <toggle-button
          v-model="editingMode"
          :labels="{checked: $t('component.common.yes'), unchecked: $t('component.common.no')}"
          :value="false"
      />
      <div v-if="editingMode">
        {{ $t('Area.choiceTypeGeometriesDrawn') }}
        <div class="col-lg-2">
          <opensilex-SelectForm
              :clearable="false"
              :multiple="false"
              :options="drawControls"
              :selected.sync="drawType"
              @select="select"
          ></opensilex-SelectForm>
        </div>
      </div>
    </div>
    <!--    <div v-if="editingAreaPopUp && editingMode">-->
    <!--      {{ memorizesArea() && (this.$bvModal.show("eventArea")) }}-->
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
      {{ memorizesArea() }}
      <opensilex-PageActions>
        <template v-slot>
          <opensilex-CreateButton v-if="user.hasCredential(credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID)"
                                  :disabled="!editingArea"
                                  label="MapView.add-button"
                                  @click="areaForm.showCreateForm()"
          ></opensilex-CreateButton>
          <opensilex-DeleteButton v-if="user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)"
                                  :small="false"
                                  label="MapView.deleteLastAreaNotValidatedButton"
                                  @click="deleteLastFieldNotValidated()"
          ></opensilex-DeleteButton>
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
          <Area :features-area="featuresArea"/>
        </template>

        <template v-if="endReceipt && editingMode">
          <div id="editionMode">
            <vl-layer-vector v-if="features.length !== 0">
              <vl-source-vector :features.sync="features" ident="the-source"></vl-source-vector>

              <vl-style-box>
                <vl-style-stroke color="#ff3620"></vl-style-stroke>
                <vl-style-fill color="rgba(255,255,255,0.5)"></vl-style-fill>
              </vl-style-box>
            </vl-layer-vector>
            <Area :features-area="featuresArea"/>

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
            :features.sync="selectedFeatures"
        />
      </vl-map>

      <div id="selectedTable">
        <b-table
            v-if="selectedFeatures.length !== 0"
            :fields="fieldsSelected"
            :items="selectedFeatures"
            hover
            striped
        >
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
import Area from "./AreaComponents.vue";
import {ExperimentGetDTO, ScientificObjectNodeDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component({
  components: {Area}
})
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
  arrayPrefix: any[] = [];
  fieldsSelected = [
    {
      key: "properties.name",
      label: "name",
      sortable: true
    },
    {
      key: "properties.uri",
      label: "uri",
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
    },
    {
      key: "properties.author",
      label: "author",
      sortable: true
    },
  ];
  selectedFeatures: any[] = [];
  nodes = [];

  private nameExperiment: string = "";
  private drawType: string = "Polygon";
  private editingMode: boolean = false;
  private editingArea: boolean = false;
  private editingAreaPopUp: boolean = false;
  private endReceipt: boolean = false;
  private drawControls = [];

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
    areaUriResult.then(areaUri => {
      console.debug("showAreaDetails", areaUri);
      location.reload();
    });
  }

  memorizesArea() {
    this.$store.state.zone = this.features[this.features.length - 1];
  }

  deleteLastFieldNotValidated() {
    this.features.splice(this.features.length - 1);
  }

  showCreateForm() {
    this.areaForm.showCreateForm();
  }

  created() {
    this.$store.state.experiment = decodeURIComponent(this.$route.params.uri);
    this.loadNameExperiment();
    this.loadDrawTypes();
    // this.loadNamespaces();

    this.service = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
    );
    this.service
        .searchScientificObjectsWithGeometryListByUris(
            this.$store.state.experiment
        )
        .then(
            (
                http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>
            ) => {
              const res = http.response.result as any;
              res.forEach((element) => {
                if (element.geometry != null) {
                  element.geometry.properties = {
                      uri:element.uri,
                      name:
                      element.name,
                      type: element.type,
                  comment: element.comment,
                    author: element.author,
                  }
                  this.features.push(element.geometry)
                }
              });
              if (res.length != 0) {
                this.endReceipt = true;
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

  editGeometry(uri: any) {
    console.debug("editArea" + uri);
    console.log("test edit")
    // this.service
    //     .getScientificObjectsBySearch(0, 1, uri)
    //     .then(
    //         (http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>) => {
    //           console.log(http.response.result);
    //           this.areaForm.showEditForm(http.response.result);
    //         }
    //     )
    //     .catch(this.$opensilex.errorHandler);
  }

  loadDrawTypes() {
    this.drawControls = [
      {
        id: 'point',
        label: this.$i18n.t("Area.point")
      },
      {
        id: 'line-string',
        label: this.$i18n.t("Area.line-string")
      },
      {
        id: 'polygon',
        label: this.$i18n.t("Area.polygon")
      },
      {
        id: 'circle',
        label: this.$i18n.t("Area.circle")
      },
      {
        id: 'undefined',
        label: this.$i18n.t("Area.stop")
      }
    ];
  }

  successMessageArea() {
    // this.$bvModal.hide("eventAnnotation");
    // this.editingAreaPopUp = false;
    this.editingArea = false;

    return this.$i18n.t("MapView.label");
  }

  definesCenter() {
    setTimeout(() => {
      let extent = this.vectorSource.$source.getExtent();
      extent[0] -= 50;
      extent[1] -= 50;
      extent[2] += 50;
      extent[3] += 50;
      this.mapView.$view.fit(extent);
    }, 400);
  }

  loadNameExperiment() {
    let service = this.$opensilex.getService(
        "opensilex.ExperimentsService"
    );

    service
        .getExperiment(this.$store.state.experiment)
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

  private featureInsert(uri: string, geometry: any, name: any, type: any, comment ?: string, author ?: string) {
    let geoJsonObject;
    if (geometry.geometry.type == "GeometryCollection") {
      geoJsonObject = geometry.geometry.geometries[0];
    } else if (geometry.geometry.type == "Polygon") {
      geoJsonObject = geometry.geometry;
    }

    if (type.indexOf("Area") === -1) {
      this.features.push({
        type: "Feature",
        properties: {
          uri: uri,
          name: name,
          type: type,
        },
        geometry: geoJsonObject,
      });
    } else {
      this.featuresArea.push({
        type: "Feature",
        properties: {
          uri: uri,
          name: name,
          type: type,
          comment: comment,
          author: author
        },
        geometry: geoJsonObject,
      });
    }
  }
}
</script>

<i18n>
    en:
      MapView:
        label: Geometry
        add-button: Input annotation
        add: Create metadata  ?
        update: Update metadata
        uri: Geometry URI
        deleteLastAreaNotValidatedButton: Delete the last non-validated area
      Area:
        editing: Yes
        selection: No
        choiceTypeGeometriesDrawn: Choice type geometries to be drawn
        add: Draw an area
        update: Update a perennial zone
        point: Point
        line-string: LineString
        polygon: Polygon
        circle: Circle
        stop: Stop drawing
    fr:
      MapView:
        label: Géométrie
        add-button: Saisir une géometrie
        add: Créer une annotation ?
        update: Mettre à jour annotation
        uri: URI de Géométrie
        deleteLastAreaNotValidatedButton: Supprimer la dernier zone non validé
      Area:
        editing: Oui
        selection: Non
        choiceTypeGeometriesDrawn: Choix du type de géométrie à dessiner
        add: Dessiner une zone
        update: Mettre à jour une zone pérenne
        point: Point
        line-string: LineString
        polygon: Polygone
        circle: Cercle
        stop: Arrêter de dessiner
</i18n>