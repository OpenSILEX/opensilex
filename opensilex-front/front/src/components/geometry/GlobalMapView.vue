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
        <b-button @click="focusOnItems" v-b-tooltip.hover="$t('GlobalMapView.focus')" variant="outline-secondary" pill>
            <opensilex-Icon :icon="'fa#crosshairs'"/>
        </b-button>
        <b-button @click="osm = !osm" v-b-tooltip.hover="$t('GlobalMapView.tile')" variant="outline-secondary" pill>
            <opensilex-Icon :icon="'ik#ik-layers'"/>
        </b-button>
        <!------------------------------------- MAP -------------------------------->
        <div data-testid="map-poster">
            <!-- Map config - "mapControls" to display the scale -->
            <vl-map ref="globalMap" :default-controls="mapControls" data-projection="EPSG:4326"
                    style="height: 800px" @click="manageCluster">
                <!-- Zoom and position -->
                <vl-view ref="globalMapView" :min-zoom="2" :max-zoom="22" @update:center="getCurrentExtent"
                         @update:zoom="disabledLayerButtons"></vl-view>
                <!-- Base tiles -->
                <vl-layer-tile v-if="osm">
                    <vl-source-osm/>
                </vl-layer-tile>
                <vl-layer-tile v-else>
                    <vl-source-bingmaps :api-key="apiKey" :imagery-set="imagerySet"></vl-source-bingmaps>
                </vl-layer-tile>
                <!-- SITE layer group-->
                <vl-layer-group v-if="isSiteLoaded && siteFeaturesDisplay.id" :visible="siteFeaturesDisplay.visibility">
                    <!-- SITE layer -->
                    <vl-layer-vector ref="siteLayerVector" :max-resolution="18" render-mode="image">
                        <vl-source-vector :features="siteFeaturesDisplay.features"
                                          @mounted="focusOnItems"></vl-source-vector>
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
                    <vl-layer-vector ref="siteClusterLayerVector" :min-resolution="18" render-mode="image">
                        <vl-source-cluster :distance="25">
                            <vl-source-vector
                                    :features="siteFeaturesDisplay.features"
                            ></vl-source-vector>
                            <vl-style-func :factory="makeSiteClusterStyleFunc"></vl-style-func>
                        </vl-source-cluster>
                    </vl-layer-vector>
                </vl-layer-group>
                <!-- FACILITY layer group-->
                <vl-layer-group v-if="isFacilityLoaded" :visible="facilityFeaturesDisplay.visibility">
                    <!-- FACILITY layer -->
                    <vl-layer-vector ref="facilityLayerVector" :max-resolution="3" render-mode="image">
                        <vl-source-vector :features="facilityFeaturesDisplay.features"></vl-source-vector>
                        <vl-style-box>
                            <vl-style-stroke
                                    :color="facilityFeaturesDisplay.style.stroke"
                                    :width="1"
                            ></vl-style-stroke>
                            <vl-style-fill
                                    :color="addTransparency(facilityFeaturesDisplay.style.fill)"
                            ></vl-style-fill>
                            <vl-style-circle :radius="8">
                                <vl-style-stroke
                                        :color="facilityFeaturesDisplay.style.stroke"
                                        :width="3"
                                ></vl-style-stroke>
                                <vl-style-fill
                                        :color="facilityFeaturesDisplay.style.fill"
                                ></vl-style-fill>
                            </vl-style-circle>
                        </vl-style-box>
                    </vl-layer-vector>
                    <!-- FACILITY cluster-->
                    <vl-layer-vector ref="facilityClusterLayerVector" :min-resolution="3" render-mode="image">
                        <vl-source-cluster :distance="25">
                            <vl-source-vector
                                    :features="facilityFeaturesDisplay.features"
                            ></vl-source-vector>
                            <vl-style-func :factory="makeFacilityClusterStyleFunc"></vl-style-func>
                        </vl-source-cluster>
                    </vl-layer-vector>
                </vl-layer-group>
                <!-- Interaction for selecting vector features -->
                <vl-interaction-select ref="selectedLayerVector" @select="selectFeature" @unselect="unSelectFeature"
                                       :filter="filterInteraction">
                    <vl-style-box>
                        <vl-style-stroke
                                color="red"
                                :width="4"
                        ></vl-style-stroke>
                        <vl-style-fill
                                :color="addTransparency('#FFFFFF')"
                        ></vl-style-fill>
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
        <opensilex-GlobalMapMenu v-if="isSiteLoaded" ref="globalMapMenu"
                                 :items="selectedLayer"
                                 :itemsInitial="selectedLayerInitial"
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
import {OrganizationsService, SiteGetWithGeometryDTO, FacilityGetWithGeometryDTO} from 'opensilex-core/index';
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
    @Ref("facilityClusterLayerVector")
    private readonly facilityClusterLayerVector!: Layer;
    @Ref("siteLayerVector")
    private readonly siteLayerVector!: Layer;
    @Ref("facilityLayerVector")
    private readonly facilityLayerVector!: Layer;
    @Ref("selectedLayerVector")
    private readonly selectedLayerVector!: Interaction;
    //endregion

    //#region Data
    //MAP
    private osm: boolean = true;
    private apiKey: string = 'ArbsA9NX-AZmebC6VyXAnDqjXk6mo2wGCmeYM8EwyDaxKfQhUYyk0jtx6hX5fpMn';
    private imagerySet: string = 'AerialWithLabels';
    private mapControls: Collection<Control> = defaultControl({
        attribution: false,
        rotate: false,
        zoom: true
    }).extend([new Attribution({collapsed: true, collapsible: true}), new ScaleLine()]);
    //LAYER - SITE
    private siteFeaturesInitial: Layer = this.getFeatures();
    private siteFeaturesDisplay: Layer = this.getFeatures();
    private isSiteLoaded: boolean = false;
    //LAYER - FACILITY
    private facilityFeaturesInitial: Layer = this.getFeatures();
    private facilityFeaturesDisplay: Layer = this.getFeatures();
    private isFacilityLoaded: boolean = false;
    //LAYER - SELECTION
    private selectedLayer: Layer = {};
    private selectedFeature: {} = {};
    private selectedLayerInitial: Layer = {};
    private stopInteraction: boolean = true;
    // TOOL BUTTONS
    private buttons: { id: string, label: string, resolution?: number, state: boolean, disabled: boolean }[] = [];
    private buttonsFeaturesMap: Map<string, {}> = new Map<string, {}>();
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
        switch (this.selectedLayer.id) {
            default:
                break;
            case this.siteFeaturesInitial.id:
                this.selectedLayerInitial = this.siteFeaturesInitial;
                break;
            case this.facilityFeaturesInitial.id:
                this.selectedLayerInitial = this.facilityFeaturesInitial;
                break;
        }
        //open the menu
        this.globalMapMenu.openGlobalMapSidebar();
    }

    //disabled button if the corresponding layer is not displayed on the map (attribute max resolution)
    private disabledLayerButtons() {
        this.buttons.forEach(btn => {
            btn.disabled = btn.resolution <= this.globalMapView.$view.getResolution();
        })
    }

    private updatedColor(color: string) {
        switch (this.selectedLayer.id) {
            default:
                return;
            case this.siteFeaturesDisplay.id:
                this.siteFeaturesDisplay.style.fill = color;
                this.siteClusterLayerVector.$layer.setStyle(this.makeSiteClusterStyleFunc())
                return;
            case this.facilityFeaturesDisplay.id:
                this.facilityFeaturesDisplay.style.fill = color;
                this.facilityClusterLayerVector.$layer.setStyle(this.makeFacilityClusterStyleFunc())
                return;
        }
    }

    private updatedVisibility(status: boolean) {
        switch (this.selectedLayer.id) {
            default:
                return;
            case this.siteFeaturesDisplay.id:
                return this.siteFeaturesDisplay.visibility = status;
            case this.facilityFeaturesDisplay.id:
                return this.facilityFeaturesDisplay.visibility = status;
        }
    }

    private updatedFeatures(features?) {
        if (features) {
            this.selectedLayer.features = this.selectedLayerInitial.features.filter(feat => features.includes(feat.id));
        } else {
            this.selectedLayer.features = this.selectedLayerInitial.features;
        }
        this.focusOnItems();
    }

    private selectFeature(feature) {
        //updateSelectedLayer
        if (this.siteFeaturesDisplay.features.find(feat => feature.getProperties().uri === feat.id)) {
            this.selectedLayer = this.siteFeaturesDisplay;
            this.selectedLayerInitial = this.siteFeaturesInitial;
        } else if (this.facilityFeaturesDisplay.features.find(feat => feature.getProperties().uri === feat.id)) {
            this.selectedLayer = this.facilityFeaturesDisplay;
            this.selectedLayerInitial = this.facilityFeaturesInitial;
        }

        this.selectedFeature = this.selectedLayer.features.find(feat => feature.getProperties().uri === feat.id)
    }

    private unSelectFeature() {
        this.selectedFeature = {}
    }

    private filterInteraction(vector) {
        if (!this.stopInteraction) {
            return vector;
        }
    }

    private selectFeatureFromMenu(feature) {
        this.selectedLayerVector.unselectAll()

        if (feature !== null) {
            let selectedLayer;

            switch (this.selectedLayer.id) {
                default:
                    return;
                case this.siteFeaturesDisplay.id:
                    selectedLayer = this.siteLayerVector;
                    break;
                case this.facilityFeaturesDisplay.id:
                    selectedLayer = this.facilityLayerVector;
                    break;
            }

            let selection = selectedLayer.$layer.getSource().getFeatures().find(feat => feat.getId() === feature.id)
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
            {id: 'site', label: 'component.common.organization.sites', state: true, disabled: false},
            {id: 'facility', label: 'component.menu.facilities', state: false, disabled: true},
        ]
    }

    private mounted() {
        this.getSitesFeatures();
        this.getFacilityFeatures();
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

    private addTransparency(color) {
        return (
                "rgba(" +
                parseInt(color.substring(1, 3), 16) +
                "," +
                parseInt(color.substring(3, 5), 16) +
                "," +
                parseInt(color.substring(5, 7), 16) +
                ",0.5)"
        );
    }

    private getFeatures() {
        return {
            id: undefined,
            title: undefined,
            features: [{
                geometry: {},
                properties: {},
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
            if (http.response.result.length > 0) {
                this.siteFeaturesInitial = this.convertObjectIntoGeoJson(results, results[0].rdf_type, this.buttons.find(button => button.id === 'site').id, '#d10bdb', '#fff');
                this.siteFeaturesDisplay = Object.assign({}, this.siteFeaturesInitial);
            }
        }).finally(() => {
            //Site by default
            this.selectedLayer = this.siteFeaturesDisplay
            this.selectedLayerInitial = this.siteFeaturesInitial
            this.buttonsFeaturesMap.set("site", this.siteFeaturesDisplay)
            this.isSiteLoaded = true;
            setTimeout(() => {
                this.globalMapMenu.openGlobalMapSidebar();
            }, 1)
        })
    }

    private getFacilityFeatures() {
        this.organizationsService.getFacilitiesWithGeometry(undefined)
                .then((http: HttpResponse<OpenSilexResponse<Array<FacilityGetWithGeometryDTO>>>) => {
                    let results: FacilityGetWithGeometryDTO[] = http.response.result;
                    this.facilityFeaturesInitial = this.convertObjectIntoGeoJson(results, this.$opensilex.Oeso.FACILITY_TYPE_URI, this.buttons.find(button => button.id === 'facility').id, '#FF7300', '#fff');
                    this.facilityFeaturesDisplay = Object.assign({}, this.facilityFeaturesInitial);
                }).finally(() => {
            this.buttonsFeaturesMap.set("facility", this.facilityFeaturesDisplay)
            this.isFacilityLoaded = true;
        })
    }

    private convertObjectIntoGeoJson(results, id, title, fillColor, strokeColor) {
        let layer = {
            id: id,
            title: title,
            features: [],
            style: {
                fill: fillColor,
                stroke: strokeColor
            },
            visibility: true
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

    private focusOnItems() {
        setTimeout(() => {
            if (this.selectedLayer.features.length > 0) {
                let selectedLayer;

                switch (this.selectedLayer.id) {
                    default:
                        return;
                    case this.siteFeaturesDisplay.id:
                        selectedLayer = this.siteLayerVector;
                        break;
                    case this.facilityFeaturesDisplay.id:
                        selectedLayer = this.facilityLayerVector;
                        break;
                }

                if (selectedLayer.getSource()) {
                    this.fitViewWithFeaturesExtent(selectedLayer.getSource().getExtent());
                }
            }
        }, 1)
    }

    // cluster points style
    private makeSiteClusterStyleFunc() {
        let styleStroke: string = this.siteFeaturesDisplay.style.stroke;
        let styleFill: string = this.siteFeaturesDisplay.style.fill;

        return this.clusterStyle(styleStroke, styleFill);
    }

    private makeFacilityClusterStyleFunc() {
        let styleStroke: string = this.facilityFeaturesDisplay.style.stroke;
        let styleFill: string = this.facilityFeaturesDisplay.style.fill;

        return this.clusterStyle(styleStroke, styleFill);
    }

    private clusterStyle(styleStroke, styleFill) {
        const styleCache = {};
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
                    } else {
                        // to avoid selecting with vl-interaction when we use the cluster on click on the map
                        this.stopInteraction = false;
                    }
                }
        )
    }

    private fitViewWithFeaturesExtent(extent) {
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
        tile: Change map background
fr:
    GlobalMapView:
        title: Carte globale
        description: Affiche les éléments géospatialisés de l'instance
        menu: Gérer les données
        focus: Zoom sur les éléments
        tile: Changer le fond de carte
</i18n>