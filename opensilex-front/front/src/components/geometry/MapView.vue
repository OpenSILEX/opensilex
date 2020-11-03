<template>
  <div id="map">
    <div>
      <h1>{{ nameExperiment }}</h1>
    </div>
    <div v-if="endReceipt" id="selectionMode">
      {{ $t("credential.geometry.instruction") }}
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
            <vl-source-vector ref="vectorSource">
              <vl-feature
                v-for="feature in features"
                :key="feature.id"
                :properties="feature.properties"
              >
                <vl-geom-polygon :coordinates="feature.geometry.coordinates" />
              </vl-feature>
            </vl-source-vector>
          </vl-layer-vector>
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
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueLayers from "vuelayers";
import { DragBox } from "ol/interaction";
import { platformModifierKeyOnly } from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
import {
  ExperimentGetDTO,
  ExperimentsService,
  ScientificObjectNodeDTO,
  ScientificObjectsService,
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class GeometryView extends Vue {
  @Ref("mapView") readonly mapView!: any;
  @Ref("vectorSource") readonly vectorSource!: any;
  $opensilex: any;
  $store: any;
  el: "map";
  service: ScientificObjectsService;
  features: any[] = [];
  arrayPrefix: any[] = [];
  fieldsSelected = [
    {
      key: "properties.name",
      label: "name",
      sortable: true,
    },
    {
      key: "properties.uri",
      label: "uri",
      sortable: true,
    },
    {
      key: "properties.type",
      label: "type",
      sortable: true,
    },
  ];
  selectedFeatures: any[] = [];
  nodes = [];

  private nameExperiment: string = "";
  private editingMode: boolean = false;
  private endReceipt: boolean = false;

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

  created() {
    this.$store.state.experiment = decodeURIComponent(this.$route.params.uri);
    this.loadNameExperiment();

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
              this.featureInsert(
                element.uri,
                element.geometry,
                element.name,
                element.type
              );
            }
          });
          if (res.length != 0) {
            this.definesCenter();
          }
        }
      )
      .catch(this.$opensilex.errorHandler);
    this.endReceipt = true;
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

  definesCenter() {
    setTimeout(() => {
      let extent = this.vectorSource.$source.getExtent();
      extent[0] -= 50;
      extent[1] -= 50;
      extent[2] += 50;
      extent[3] += 50;
      this.mapView.$view.fit(extent);
    }, 1000);
  }

  loadNameExperiment() {
    let service: ExperimentsService = this.$opensilex.getService(
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

  private featureInsert(uri: string, geometry: any, name: any, type: any) {
    let geoJsonObject;
    if (geometry.geometry.type == "GeometryCollection") {
      geoJsonObject = geometry.geometry.geometries[0];
    } else if (geometry.geometry.type == "Polygon") {
      geoJsonObject = geometry.geometry;
    }
    this.features.push({
      type: "Feature",
      properties: {
        uri: uri,
        name: name,
        type: type,
      },
      geometry: geoJsonObject,
    });
  }
}

// all input/output coordinates, GeoJSON features in EPSG:4326 projection
Vue.use(VueLayers, {
  dataProjection: "EPSG:4326",
});
</script>

<i18n>
    en:
     MapView:
      label: Geometry
      add-button: Input annotation
      add: Create metadata
      update: Update metadata
      uri: Geometry URI
    fr:
     MapView:
      label: Géométrie
      add-button: Saisir une géometrie
      add: Créer une annotation
      update: Mettre à jour annotation
      uri: URI de Géométrie
</i18n>
