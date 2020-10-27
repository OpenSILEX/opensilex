<template>
  <div id="map">
    <div>
      <h1>{{ nameExperiment }}</h1>
    </div>
    <div id="selectionMode">
      {{ $t('credential.geometry.instruction') }}
      <vl-map
          :load-tiles-while-animating="true"
          :load-tiles-while-interacting="true"
          data-projection="EPSG:4326"
          style="height: 400px"
          @created="mapCreated"
      >
        <vl-view :center.sync="center" :rotation.sync="rotation" :zoom.sync="zoom"></vl-view>

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
                <vl-geom-polygon :coordinates="feature.geometry.coordinates"/>
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
import {Component} from "vue-property-decorator";
import Vue from "vue";
import VueLayers from "vuelayers";
import "vuelayers/lib/style.css"; // needs css-loader
import {DragBox} from "ol/interaction";
import {platformModifierKeyOnly} from "ol/events/condition";
import * as olExt from "vuelayers/lib/ol-ext";
import {
  ExperimentGetDTO,
  ExperimentsService,
  ScientificObjectNodeDTO,
  ScientificObjectsService
} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from 'opensilex-core/HttpResponse';

@Component
export default class GeometryView extends Vue {
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
    }
    // {
    //   key: "properties.comment",
    //   label: "comment",
    //   sortable: true
    // },
    // {
    //   key: "properties.author",
    //   label: "author",
    //   sortable: true
    // },
    // {
    //   key: "actions",
    //   label: "actions"
    // }
  ];
  selectedFeatures: any[] = [];
  center: number[] = [3.9735156, 43.612549];
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

  static async asyncInit($opensilex: OpenSilexVuePlugin) {
    await $opensilex.loadModule("opensilex-core");
  }

  data() {
    return {
      zoom: 18,
      rotation: 0
    };
  }

  created() {
    this.$store.state.experiment = decodeURIComponent(this.$route.params.uri);
    this.loadNameExperiment();
    // this.loadNamespaces();
    console.log("Loading form view...");

    this.service = this.$opensilex.getService(
        "opensilex.ScientificObjectsService"
    );
    this.service.searchScientificObjectsWithGeometryListByUris(
        this.$store.state.experiment,
    ).then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
          const res = http.response.result as any;
          res.forEach(element => {
            if (element.geometry != null) {
              this.featureInsert(
                  element.uri,
                  element.geometry,
                  element.name,
                  element.type
              );
            }
          });
        }
    ).catch(this.$opensilex.errorHandler);
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

  loadNameExperiment() {
    let service: ExperimentsService = this.$opensilex.getService(
        "opensilex.ExperimentsService"
    );

    service.getExperiment(this.$store.state.experiment)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          this.nameExperiment = http.response.result.label;
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
  }

  select(value) {
    this.$emit("select", value);
  }

  // loadNamespaces() {
  //   let service: VocabulariesService = this.$opensilex.getService(
  //       "opensilex.VocabulariesService"
  //   );
  //
  //   service.getNamespaces(
  //       200,
  //       0
  //   )
  //       .then(
  //           (http: HttpResponse<OpenSilexResponse<Array<Property>>>) => {
  //             this.arrayPrefix = http.response.result;
  //           }
  //       )
  //       .catch(this.$opensilex.errorHandler);
  // }

  private featureInsert(uri: string, geometry: any, name: any, type: any) {
    this.features.push({
      type: "Feature",
      properties: {
        uri: uri,
        name: name,
        type: type,
      },
      geometry: geometry.geometry.geometries[0]
    });
  }
}

// all input/output coordinates, GeoJSON features in EPSG:4326 projection
Vue.use(VueLayers, {
  dataProjection: "EPSG:4326"
});
</script>

<!--<i18n>-->
<!--    en:-->
<!--    GeometryView:-->
<!--    fr:-->
<!--    GeometryView:-->
<!--</i18n>-->
