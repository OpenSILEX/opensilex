<!--
  - ******************************************************************************
  -                         GlobalMapView.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 12/08/2024 14:05
  - Contact: alexia.chiavarino@inrae.fr
  - ******************************************************************************
  -
  -->

<template>
    <div data-testid="global-map-view">
        <!-- map panel button (toggle side bar) -->
        <b-button-group>
            <b-button
                    v-for="(btn, idx) in buttons"
                    :key="idx"
                    @click="openGlobalMapMenu(btn.id)"
                    :pressed.sync="btn.state"
                    :disabled="btn.disabled"
                    variant="outline-secondary"
                    pill
            >
                {{ $t(btn.label) }}
            </b-button>
        </b-button-group>
        <!-- tool buttons -->
        <b-button @click="focusOnSites" v-b-tooltip.hover="$t('GlobalMapView.focus')" variant="outline-secondary" pill><opensilex-Icon :icon="'fa#crosshairs'" /></b-button>
        <b-button @click="osm = !osm" v-b-tooltip.hover="$t('GlobalMapView.tile')" variant="outline-secondary" pill><opensilex-Icon :icon="'ik#ik-layers'" /></b-button>
        <!------------------------------------- MAP -------------------------------->
        <div data-testid="map-poster">
            <!-- Map config - "mapControls" to display the scale -->
            <vl-map ref="globalMap" :default-controls="mapControls" data-projection="EPSG:4326"
                    style="height: 800px" @click="manageCluster">
                <!-- Zoom and position -->
                <vl-view ref="globalMapView" :min-zoom="2" :max-zoom="22" @update:center="getCurrentExtent"></vl-view>
                <!-- Base tiles -->
                <vl-layer-tile v-if="osm">
                    <vl-source-osm/>
                </vl-layer-tile>
                <vl-layer-tile v-else>
                    <vl-source-bingmaps :api-key="apiKey" :imagery-set="imagerySet"></vl-source-bingmaps>
                </vl-layer-tile>
                <!-- SITE layer group-->
                <vl-layer-group v-if="isMapMounted && siteFeaturesDisplay.id" :visible="siteVisibility">
                    <!-- SITE layer -->
                    <vl-layer-vector id="site-layer-vector" ref="siteLayerVector" :max-resolution="18" render-mode="image">
                        <vl-source-vector :features="siteFeaturesDisplay.features" @mounted="focusOnSites"></vl-source-vector>
                        <vl-style-box>
                            <vl-style-circle :radius="8">
                                <vl-style-stroke
                                        :color="siteFeaturesDisplay.style.stroke"
                                        :width="3"
                                ></vl-style-stroke>
                                <vl-style-fill
                                        :color="siteFeaturesDisplay.style.fill"
                                ></vl-style-fill>
                            </vl-style-circle>
                        </vl-style-box>
                    </vl-layer-vector>
                    <!-- SITE cluster-->
                    <vl-layer-vector id="site-cluster-layer" ref="siteClusterLayerVector" :min-resolution="18" render-mode="image">
                        <vl-source-cluster :distance="25">
                            <vl-source-vector
                                    :features="siteFeaturesDisplay.features"
                            ></vl-source-vector>
                            <vl-style-func :factory="makeClusterStyleFunc"></vl-style-func>
                        </vl-source-cluster>
                    </vl-layer-vector>
                </vl-layer-group>
                <!-- Interaction for selecting vector features -->
                <vl-interaction-select ref="selectedLayerVector" @select="selectFeature" @unselect="unSelectFeature">
                    <vl-style-box>
                        <vl-style-circle :radius="8">
                            <vl-style-stroke
                                    color="red"
                                    :width="4"
                            ></vl-style-stroke>
                            <vl-style-fill
                                    color="white"
                            ></vl-style-fill>
                        </vl-style-circle>
                    </vl-style-box>
                </vl-interaction-select>
            </vl-map>
        </div>
        <opensilex-GlobalMapMenu v-if="isMapMounted" id="global-map-menu" ref="globalMapMenu"
                                 :items="siteFeaturesDisplay"
                                 :itemsInitial="siteFeaturesInitial"
                                 :selectedItem="selectedFeature"
                                 @updatedColor="updatedColor"
                                 @updatedVisibility="updatedVisibility"
                                 @onSearch="updatedFeatures"
                                 @onSelectItem="selectFeatureFromMenu"
        ></opensilex-GlobalMapMenu>
    </div>
</template>

<script lang="ts">
import Vue from 'vue';
import {Component, Ref} from 'vue-property-decorator';
import {defaults as defaultControl, ScaleLine, Control, Attribution} from "ol/control";
import {Collection} from "ol";
import {Circle as CircleStyle, Fill, Stroke, Style, Text} from "ol/style";
import * as olExtent from "ol/extent";
import Point from "ol/geom/Point";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {View, Map as mapView} from 'vuelayers/src/component/map';
import {Layer} from 'vuelayers/src/component/vector-layer';
import {Interaction} from 'vuelayers/src/component/select-interaction';
import {transformExtent} from "vuelayers/src/ol-ext/proj";
import GlobalMapMenu from "../../components/geometry/GlobalMapMenu.vue";
import {OrganizationsService, SiteGetWithGeometryDTO} from 'opensilex-core/index';
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component
export default class GlobalMapView extends Vue {
    //#region Events
    //endregion
    //#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin;
    private organizationsService: OrganizationsService;
    //endregion

    //#region Props
    //endregion

    //#region Refs
    @Ref("globalMap")
    private globalMap!: mapView;
    @Ref("globalMapView")
    private globalMapView!: View;
    @Ref("globalMapMenu")
    private readonly globalMapMenu!: GlobalMapMenu;
    @Ref("siteClusterLayerVector")
    private readonly siteClusterLayerVector!: Layer;
    @Ref("siteLayerVector")
    private readonly siteLayerVector!: Layer;
    @Ref("selectedLayerVector")
    private readonly selectedLayerVector!: Interaction;
    //endregion

    //#region Data
    private osm: boolean = true;
    private apiKey: string = 'ArbsA9NX-AZmebC6VyXAnDqjXk6mo2wGCmeYM8EwyDaxKfQhUYyk0jtx6hX5fpMn';
    private imagerySet: string ='AerialWithLabels';
    private mapControls: Collection<Control> = defaultControl({
        attribution: false,
        rotate: false,
        zoom: true
    }).extend([new Attribution({collapsed: true, collapsible: true}), new ScaleLine()]);
    private siteFeaturesInitial = this.getFeatures();
    private siteFeaturesDisplay = this.getFeatures();
    private isMapMounted: boolean = false;
    private siteVisibility: boolean = true;
    private buttons :{ id: string,label: string, resolution? : number, state: boolean, disabled: boolean}[] = [];
    private buttonsFeaturesMap : Map<string, {}> = new Map<string, {}>();
    private selectedLayer: Layer = {};
    private selectedFeature: {} = {};
    //endregion

    //#region Computed
    //endregion

    //#region Events handlers
    private openGlobalMapMenu(id) {
        //select button - deselect others
        this.buttons.forEach(btn => {
            btn.state = btn.id === id;
        })
        //set the corresponding list
        this.selectedLayer = this.buttonsFeaturesMap.get(id);
        //open the menu
        let label = this.buttons.find(button => button.id === id).label
        this.globalMapMenu.openGlobalMapSidebar(label);
    }

    //disabled button if the corresponding layer is not displayed on the map (attribute max resolution)
    private disabledLayerButtons(){
        this.buttons.forEach(btn => {
            btn.disabled = btn.resolution <= this.globalMapView.$view.getResolution();
        })
    }

    private updatedColor(color: string) {
        this.siteFeaturesDisplay.style.fill = color;
        this.siteClusterLayerVector.$layer.setStyle(this.makeClusterStyleFunc())
    }

    private updatedVisibility(status: boolean) {
        this.siteVisibility = status;
    }

    private updatedFeatures(features?) {
        if(features){
            this.siteFeaturesDisplay.features = this.siteFeaturesInitial.features.filter(feat => features.includes(feat.id));
        } else {
            this.siteFeaturesDisplay.features = this.siteFeaturesInitial.features;
        }
        this.focusOnSites();
    }

    private selectFeature(feature){
        this.selectedFeature = this.selectedLayer.features.find(feat =>feature.getProperties().uri === feat.id)
    }

    private unSelectFeature(){
        this.selectedFeature = {}
    }

    private selectFeatureFromMenu(feature){
        this.selectedLayerVector.unselectAll()

        if(feature !== null){
            let selectedLayer;
            if(this.selectedLayer.title === "site"){
                selectedLayer = this.siteLayerVector;
            }
            let selection = selectedLayer.$layer.getSource().getFeatures().find(feat=> feat.getId()===feature.id)
            this.selectedLayerVector.select(selection);
            this.fitViewWithFeaturesExtent(selection.getGeometry().getExtent());
        }
    }
    //endregion

    //#region Public methods
    //endregion

    //#region Hooks
    private created() {
        this.organizationsService = this.$opensilex.getService("opensilex.OrganizationsService");

        this.buttons = [
            { id:'site',label: 'component.common.organization.sites', state: true, disabled: false },
        ]
    }

    private mounted() {
        this.getSitesFeatures();
    }
    //endregion

    //#region Private methods
    private getCurrentExtent() {
        return transformExtent(
                this.globalMapView.$view.calculateExtent(),
                "EPSG:3857",
                "EPSG:4326"
        );
    }

    private getFeatures(){
        return {
            id: undefined,
            title: undefined,
            features: [{
                geometry: {},
                properties:  {},
                id: undefined
            }],
            style: {
                fill: undefined,
                stroke: undefined
            }
        }
    }

    private getSitesFeatures() {
        this.organizationsService.getSitesWithLocation().then((http: HttpResponse<OpenSilexResponse<Array<SiteGetWithGeometryDTO>>>) => {
            let results: SiteGetWithGeometryDTO[] = http.response.result;
            if(http.response.result.length > 0){
              this.siteFeaturesInitial = this.convertObjectIntoGeoJson(results, results[0].rdf_type, this.buttons.find(button => button.id === 'site').id, '#d10bdb', '#fff');
              this.siteFeaturesDisplay = Object.assign({}, this.siteFeaturesInitial);
            }
        }).finally(() => {
            this.selectedLayer = this.siteFeaturesDisplay
            this.buttonsFeaturesMap.set("site", this.siteFeaturesDisplay)
            this.isMapMounted = true;
            setTimeout(() => {
                this.globalMapMenu.openGlobalMapSidebar('component.common.organization.sites');
            }, 1)
        })
    }

    private convertObjectIntoGeoJson(results, id, title, fillColor, strokeColor) {
        let layer = {
            id: id,
            title: title,
            features: [],
            style: {
                fill: fillColor ,
                stroke: strokeColor
            }
        };

        results.forEach(result => {
            let feature = result.geometry;
            feature.properties = result;
            feature.id = result.uri;
            delete feature.properties.geometry;
            layer.features.push(feature)
        })
        return layer;
    }

    private focusOnSites(){
      setTimeout(()=>{
          if(this.siteFeaturesDisplay.features.length > 0 && this.siteLayerVector.getSource()){
              this.fitViewWithFeaturesExtent(this.siteLayerVector.getSource().getExtent());
          }
      },1)
    }

    // cluster points style
    private makeClusterStyleFunc() {
        const styleCache = {};
        let styleStroke: string = this.siteFeaturesDisplay.style.stroke;
        let styleFill: string = this.siteFeaturesDisplay.style.fill;

        return function __clusterStyleFunc(feature) {
            const size = feature.get('features').length;
            let style = styleCache[size];
            if (!style) {
                style = new Style({
                    image: new CircleStyle({
                        radius: 10,
                        stroke: new Stroke({
                            color: styleStroke,
                            width: 3
                        }),
                        fill: new Fill({
                            color: styleFill,
                        }),
                    }),
                    text: new Text({
                        text: size.toString(),
                        fill: new Fill({
                            color: '#fff',
                        }),
                    }),
                });
                styleCache[size] = style;
            }
            return style;
        }
    }

    // manage behavior on click on cluster point -> zoom in
    private manageCluster(e) {
        this.globalMap.forEachFeatureAtPixel(
                e.pixel,
                (feature) => {
                    //transform all geometries into points and build the new extent
                    if (feature.get('features')) {
                        let points = [];
                        let features = feature.get('features');

                        features.forEach((feat) => {
                            let geom = feat.getGeometry();
                            if (geom.getType() == 'Point') {
                                points.push(geom.getCoordinates());
                            } else if (geom.getType() == 'Polygon') {
                                points.push(geom.getInteriorPoint().getCoordinates());
                            } else if (geom.getType() == 'LineString') {
                                let point = new Point(geom.getCoordinateAt(0.5), undefined);
                                points.push(point.getCoordinates());
                            }
                        })
                        this.fitViewWithFeaturesExtent(olExtent.boundingExtent(points));
                    }
                }
        )
    }

    private fitViewWithFeaturesExtent(extent){
        this.globalMapView.$view.fit(extent, {maxZoom: 17});
    }
    //endregion
}
</script>

<style scoped lang="scss">

::v-deep .ol-zoom {
    left: auto;
    right: 8px;
}

.btn {
    margin: 3px;
}
</style>

<i18n>
en:
    GlobalMapView:
        title: Global map
        description: Displays geospatialized elements of the instance
        menu: Manage data
        focus: Focus on items
        tile : Change map background
fr:
    GlobalMapView:
        title: Carte globale
        description: Affiche les éléments géospatialisés de l'instance
        menu: Gérer les données
        focus: Zoom sur les éléments
        tile : Changer le fond de carte
</i18n>