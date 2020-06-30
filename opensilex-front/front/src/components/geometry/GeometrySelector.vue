<template>

  <b-modal ref="modalRef" size="xxl" :static="true">
    
    <template v-slot:modal-title>
      <i class="ik ik-search mr-1"></i> {{ $t('GeometrySelector.selectLabel') }}
    </template>

    <template v-slot:modal-footer>
      <button type="button" class="btn btn-secondary" v-on:click="hide(false)">{{ $t('component.common.close') }}</button>
      <button type="button" class="btn btn-primary" v-on:click="hide(true)">{{ $t('component.common.validateSelection') }}</button>
    </template>

    <div class="card">

        <opensilex-SearchFilterField
            @search="refreshMap()"
            @clear="resetFilters()"
        >
        
            <template v-slot:filters>
                
                <!-- Longitude -->
                <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.longitude"
                        label="GeometrySelector.longitude"
                        type="text"
                    ></opensilex-InputForm>
                </opensilex-FilterField>

                <!-- Latitude -->
                <opensilex-FilterField>
                    <opensilex-InputForm
                        :value.sync="filter.latitude"
                        label="GeometrySelector.latitude"
                        type="text"
                    ></opensilex-InputForm>
                </opensilex-FilterField>

            </template>
            
        </opensilex-SearchFilterField>

        <vl-map ref="map" :load-tiles-while-animating="true" :load-tiles-while-interacting="true" data-projection="EPSG:4326" style="height: 400px" @created="mapCreated">
            <vl-view :zoom="5" :center="center" :rotation="0"></vl-view>

            <vl-layer-tile id="osm">
                <vl-source-osm></vl-source-osm>
            </vl-layer-tile>

            <vl-layer-vector>
                <vl-source-vector ref="vectorSource" :features.sync="features" ident="vectorSource"></vl-source-vector>

                <vl-style-box>
                    <vl-style-stroke color="green"></vl-style-stroke>
                    <vl-style-fill color="rgba(255,255,255,0.5)"></vl-style-fill>
                </vl-style-box>
            </vl-layer-vector>

            <vl-interaction-draw type="Polygon" source="vectorSource" @drawend="refreshTable">
                <vl-style-box>
                    <vl-style-stroke color="blue"></vl-style-stroke>
                    <vl-style-fill color="rgba(255,255,255,0.5)"></vl-style-fill>
                </vl-style-box>
            </vl-interaction-draw>

        </vl-map>

        <opensilex-TableAsyncView
            ref="tableRef" 
            :searchMethod="searchFeatures" 
            :fields="fields"
            labelNumberOfSelectedRow="component.experiment.search.selectedLabel"
            iconNumberOfSelectedRow="ik#ik-layers"
            defaultPageSize="10">

            <template v-slot:cell(actions)="{data}">
                <a href="#" class="btn btn-icon btn-row-action btn-outline-primary" :title="$t('GeometrySelector.view')" v-on:click="viewFeature(data.item)"><i class="ik ik-eye"></i></a>
                <a href="#" class="btn btn-icon btn-row-action btn-outline-danger" :title="$t('GeometrySelector.delete')" v-on:click="removeFeature(data.item)"><i class="ik ik-x"></i></a>
            </template>

        </opensilex-TableAsyncView>
    </div>

    </b-modal>

</template>

<script lang="ts">
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";

    import VueLayers from "vuelayers";
    import "vuelayers/lib/style.css";
    import * as olExt from "vuelayers/lib/ol-ext";

    import * as ol from "ol";
    import TileLayer from 'ol/layer/Tile';
    import {OSM, Vector as VectorSource} from 'ol/source';
    import {fromLonLat} from 'ol/proj';
    import Draw from "ol/interaction/Draw";
    import {platformModifierKeyOnly} from "ol/events/condition";

    class GeometrySelectorFilter {
        longitude;
        latitude;

        constructor() {
            this.reset();
        }

        reset() {
            this.longitude = "1.9459951249999952";
            this.latitude = "46.69019317574475";
        }

    }

    @Component
    export default class GeometrySelector extends Vue {
        $opensilex: any;
        $store: any;

        @Ref("map") readonly map!: any;
        @Ref("vectorSource") readonly vectorSource!: any;

        @Ref("tableRef") readonly tableRef!: any;

        center = [0, 0];

        filter:GeometrySelectorFilter = new GeometrySelectorFilter();

        fields = [
            {
                key: "geometry",
                label: "GeometrySelector.geometry",
                sortable: true
            }, {
                key: "actions",
                label: "GeometrySelector.actions",
                sortable: false
            }
        ];

        features: any;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        searchFeatures(options) {            
            return new Promise((resolve, reject) => {
                let result = {
                    response: {
                        metadata: {
                            pagination: {
                                pageSize: options.pageSize,
                                totalCount: this.vectorSource?this.vectorSource.featuresDataProj.length:0
                            }
                        },
                        result: this.vectorSource?this.vectorSource.featuresDataProj:null
                    }
                };
                resolve(result);
            });
        }

        refreshTable() {
            if(this.tableRef) {
                setTimeout(this.tableRef.refresh(), 5000);
            }
        }

        viewFeature(feature) {
            console.log(feature);
            console.log(this.vectorSource);
            this.filter.longitude = feature.geometry.coordinates[0][0][0];
            this.filter.latitude = feature.geometry.coordinates[0][0][1];
            this.refreshMap();
        }

        removeFeature(feature) {
            if(this.vectorSource) {
                this.vectorSource.removeFeature(this.vectorSource.getFeatureById(feature.id));
            }
            this.refreshTable();
        }

        refreshMap() {
            this.center = [parseFloat(this.filter.longitude), parseFloat(this.filter.latitude)];
            this.refreshTable();
        }

        resetFilters() {
            this.filter.reset();
            this.refreshMap();
        }

        show() {
            let modalRef: any = this.$refs.modalRef;
            modalRef.show();

            if(this.map) {
                this.map.recreate();
                this.refreshMap();
            }
        }

        hide(validate: boolean) {
            let modalRef: any = this.$refs.modalRef;
            modalRef.hide();

            if(validate && this.map) {
                this.$emit("onValidate", this.vectorSource.featuresDataProj);
            }
        }
    }

    Vue.use(VueLayers, {
        dataProjection: 'EPSG:4326',
    });

</script>

<style scoped lang="scss">
    .map {
        height: 400px;
        width: 100%;
    }
</style>

<i18n>

en:
  GeometrySelector:
    longitude: Longitude
    latitude: Latitude
    geometry: Geometry
    actions: Actions
    selectLabel: Select Spacial Positions
    view: View Spacial Positions
    delete: Delete Spacial Positions

fr:
  GeometrySelector:
    longitude: Longitude
    latitude: Latitude
    geometry: Géométries
    actions: Actions
    selectLabel: Sélectionner des Positions Spaciales
    view: Voir les Positions Spaciales
    delete: Supprimer les Positions Spaciales

</i18n>
