<!--
  - ******************************************************************************
  -                         GlobalMapMenu.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 12/08/2024 14:12
  - Contact: alexia.chiavarino@inrae.fr
  - ******************************************************************************
  -
  -->

<template>
    <!---------------------------- GLOBAL MAP MENU SIDEBAR ----------------------------->
    <b-sidebar data-testid="global-map-sidebar" ref="globalMapSidebar" v-model="isSidebarOpen" visible no-header
               class="sidebar-content">
        <template #default="{ hide }">
            <div class="b-sidebar-header header-brand opensilex-sidebar-header">
                <div class="d-flex">
                    <span data-testid="global-map-sidebar-title" class="text mr-auto"> {{
                            $t("title").toUpperCase()
                        }} </span>
                    <button data-testid="global-map-sidebar-close-button"
                            class="hamburger hamburger-collapse is-active p-3"
                            @click="hide">
                        <span class="hamburger-box">
                          <span class="hamburger-inner"></span>
                        </span>
                    </button>
                </div>
            </div>
            <b-tabs fill justified>
                <!-- LIST TAB-->
                <b-tab :title="$t('list')" active>
                    <b-container>
                        <b-row class="tab-list-header">
                            <b-col class="truncate">
                                <opensilex-Icon :icon="$opensilex.getRDFIcon(items.id)" style="font-size: 2em"/>
                                <span class="tab-list-title">{{ $t(items.title).toUpperCase() }}</span>
                            </b-col>
                            <b-col cols="5">
                                <b-button-group size="sm">
                                    <!-- VISIBILITY -->
                                    <b-check switch :checked="items.visibility" @change="updatedVisibility"
                                             style="padding-top: 5px"></b-check>
                                    <!-- COLOR -->
                                    <opensilex-InputForm type="color" :value="items.style.fill"
                                                         @update:value="updatedColor"
                                                         style="width: 40px"></opensilex-InputForm>
                                </b-button-group>
                            </b-col>
                        </b-row>
                    </b-container>
                    <p v-if="items.id === undefined">{{ $t("no-list", {itemType: $t(items.title)}).toUpperCase() }} </p>
                    <b-list-group v-else>
                        <b-list-group-item button v-for="item in items.features" :key="item.id"
                                           @click="selectItem(item,$event)" class="tab-list-group-item"
                                           :class="{ 'selected' : selectedItem === item, '': selectedItem !== item}">
                            <b-container>
                                <b-row>
                                    <b-col cols="10" style="padding-left:0" class="truncate">
                                        <p class="capitalize-first-letter tab-list-group-item-label truncate">
                                            {{ item.properties.name }}</p>
                                    </b-col>
                                    <b-col cols="2">
                                        <!-- DETAIL -->
                                        <opensilex-DetailButton
                                                label="component.account.details"
                                                v-b-toggle="item.id"
                                                :small="true"
                                                style="padding: 6px 10px"
                                                @click="getDetails(item)"
                                        ></opensilex-DetailButton>
                                    </b-col>
                                </b-row>
                                <b-row>
                                    <b-collapse :id="item.properties.uri">
                                        <!-- Address -->
                                        <opensilex-AddressView
                                                v-if="item.properties.address"
                                                :address="item.properties.address"
                                        ></opensilex-AddressView>
                                        <!-- Geometry -->
                                        <opensilex-GeometryCopy v-else :value="item.geometry"></opensilex-GeometryCopy>
                                        <!-- Facilities -->
                                        <div v-if="item.properties.facilities && item.properties.facilities.length >0">
                                            <br>
                                            <span class="field-view-title">{{ $t("component.menu.facilities") }}</span>
                                            <ul>
                                                <li v-for="facility in item.properties.facilities" :key="facility.uri">
                                                    <opensilex-UriLink
                                                            :to="{ path: '/facility/details/' + encodeURIComponent(facility.uri) }"
                                                            :uri="facility.uri"
                                                            :value="facility.name"
                                                            target="_blank"
                                                    ></opensilex-UriLink>
                                                </li>
                                            </ul>
                                        </div>
                                        <div v-else></div>
                                    </b-collapse>
                                </b-row>
                            </b-container>
                        </b-list-group-item>
                    </b-list-group>
                </b-tab>

                <!-- FILTERS TAB -->
                <b-tab :title="$t('filters')">
                    <Transition>
                        <opensilex-SearchFilterField
                                label="component.experiment.search.label"
                                @search="search"
                                @clear="reset"
                        >
                            <template v-slot:filters>
                                <font-awesome-icon
                                        icon="question-circle"
                                        class="filterHelp"
                                        v-b-tooltip.hover.top="$t('filter-help')"
                                />
                                <!-- Projects -->
                                <div>
                                    <opensilex-FilterField>
                                        <opensilex-ProjectSelector
                                                label="component.experiment.projects"
                                                :projects.sync="filters.projects"
                                                :multiple="true"
                                                class="filter-form"
                                        ></opensilex-ProjectSelector>
                                    </opensilex-FilterField>
                                </div>
                                <!-- Species -->
                                <div>
                                    <opensilex-FilterField>
                                        <opensilex-SpeciesSelector
                                                label="component.experiment.species"
                                                :selected.sync="filters.species"
                                                :multiple="true"
                                                class="filter-form"
                                        ></opensilex-SpeciesSelector>
                                    </opensilex-FilterField>
                                </div>
                                <!-- Year -->
                                <div>
                                    <opensilex-FilterField>
                                        <label>{{ $t("component.common.year") }}</label>
                                        <opensilex-StringFilter
                                                placeholder="component.common.year"
                                                :filter.sync="filters.year"
                                                type="number"
                                                class="filter-form"
                                        ></opensilex-StringFilter>
                                    </opensilex-FilterField>
                                </div>
                            </template>
                        </opensilex-SearchFilterField>
                    </Transition>
                </b-tab>
            </b-tabs>
        </template>
    </b-sidebar>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop, Watch} from "vue-property-decorator";
import {
    ExperimentGetListDTO,
    ExperimentsService,
    SiteGetDTO,
    OrganizationsService,
    FacilityGetDTO
} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component({})
export default class GlobalMapMenu extends Vue {
    //#region Plugins and services
    private readonly $opensilex: OpenSilexVuePlugin;
    private experimentsService: ExperimentsService;
    private organizationsService: OrganizationsService;
    //endregion

    //#region Props
    @Prop({
        default: (() => {
        })
    })
    private readonly items;
    @Prop({
        default: (() => {
        })
    })
    private readonly itemsInitial;
    @Prop({
        default: (() => {
        })
    })
    private readonly selectedItem;
    //endregion

    //#region Refs
    //endregion

    //#region Data
    private langUnwatcher;
    private isSidebarOpen: boolean = true;
    private filters = this.getFilters();
    //endregion

    //#region Computed
    @Watch("selectedItem")
    putOnTopListSelectedItem() {
        this.items.features.forEach((item, i) => {
            if (item.id === this.selectedItem.id) {
                this.items.features.splice(i, 1)
                this.items.features.unshift(item)
            }
        })
    }
    //endregion

    //#region Events
    private getDetails(item) {
        if(this.items.title === "facility" || this.items.title === "site"){
            this.$opensilex.showLoader();
            if (this.items.title === "site") {
                if (!item.properties.address) {
                    this.organizationsService.getSite(item.properties.uri).then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
                        let result: SiteGetDTO = http.response.result;

                        item.properties.address = result.address;
                        item.properties.facilities = result.facilities
                    })
                }
            } else if (this.items.title === "facility") {
                if (item.properties.address && !item.properties.address.countryName) {
                    this.organizationsService.getFacility(item.properties.uri).then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
                        let result: FacilityGetDTO = http.response.result;

                        item.properties.address = result.address;
                    })
                }
            }
            this.$opensilex.hideLoader();
        }
    }

    private updatedColor(event) {
        this.$emit("updatedColor", event)
    }

    private updatedVisibility(event) {
        this.$emit("updatedVisibility", event)
    }

    private selectItem(item, event) {
        // stop event from detailButton
        const target = event.target
        if (target.matches('button') || target.matches('path') || target.matches('svg') || target.matches('span')) {
            return;
        }
        // deselect item
        if (this.selectedItem !== null && this.selectedItem === item) {
            item = null;
        }

        this.$emit("onSelectItem", item)
    }

    private onSearch(features?) {
        this.$emit("onSearch", features)
    }
    //endregion

    //#region Events handlers
    private reset() {
        this.filters = this.getFilters();
        this.onSearch();
    }
    //endregion

    //#region Public methods
    public openGlobalMapSidebar() {
        this.isSidebarOpen = true;
    }
    //endregion

    //#region Hooks
    private created() {
        this.organizationsService = this.$opensilex.getService("opensilex.OrganizationsService");
        this.experimentsService = this.$opensilex.getService("opensilex.ExperimentsService")
    }

    private mounted() {
        this.langUnwatcher = this.$store.watch(
                () => this.$store.getters.language,
                (lang) => {
                    this.search();
                }
        );
    }

    private beforeDestroy() {
        this.langUnwatcher();
    }
    //endregion

    //#region Private methods
    private getFilters() {
        return {
            year: undefined,
            projects: [],
            species: []
        }
    }

    private search() {
        if (this.filters.projects.length === 0 && this.filters.species.length === 0 && !this.filters.year) {
            this.onSearch();
        } else {
            let facilitiesURI = [];
            let sitesFacilitiesMap: Map<string, string[]> = new Map();

            if (this.items.title === "site") {
                this.itemsInitial.features.forEach(feature => {
                    if (feature.properties.facilities) {
                        let facilities = feature.properties.facilities.map(facility => {
                            if (facility.uri) {
                                return facility.uri
                            } else {
                                return facility
                            }
                        })

                        sitesFacilitiesMap.set(feature.properties.uri, facilities)
                    }
                })
                facilitiesURI = [...new Set(sitesFacilitiesMap.values())].flat();
            } else if (this.items.title === "facility") {
                facilitiesURI = this.itemsInitial.features.map(facility => {
                    return facility.id
                })
            }

            //SEARCH
            this.experimentsService.searchExperiments(
                    undefined,
                    this.filters.year,
                    undefined,
                    this.filters.species,
                    undefined,
                    this.filters.projects,
                    undefined,
                    facilitiesURI,
                    undefined,
                    undefined,
                    undefined
            ).then((http: HttpResponse<OpenSilexResponse<Array<ExperimentGetListDTO>>>) => {
                let features = [];
                if (http.response.result) {
                    let facilitiesResult = [];
                    http.response.result.forEach(xp => {
                        facilitiesResult.push(xp.facilities)
                    })
                    let facilitiesSingle = [...new Set(facilitiesResult.flat())]

                    if (this.items.title === "site") {
                        facilitiesSingle.forEach(fac => {
                            for (let [site, facility] of sitesFacilitiesMap) {
                                let filter = facility.filter(s => s === fac)
                                if (filter.length > 0) {
                                    features.push(site)
                                }
                            }
                        })
                    } else if (this.items.title === "facility") {
                        features = facilitiesSingle.map(facility => {
                            return this.$opensilex.getLongUri(facility)
                        });
                    }
                } else {
                    features = [];
                }
                this.onSearch(features)
            }).catch(this.$opensilex.errorHandler)
        }
    }
    //endregion
}
</script>

<style scoped lang="scss">

::v-deep .b-sidebar {
    width: 250px;
}

.b-sidebar-outer {
    z-index: 1045;
}

.opensilex-sidebar-header {
    width: 99%;
    height: 60px;
}

.wrapper .header-brand .text {
    margin-top: auto;
}

::v-deep div.b-sidebar-header {
    background-color: #00a38d;
    color: #ffffff;
    font-size: 1.25rem;
}

.hamburger.is-active .hamburger-inner,
.hamburger.is-active .hamburger-inner::after,
.hamburger.is-active .hamburger-inner::before {
    background-color: #fff;
}

.tab-list-header {
    height: 50px;
    margin-top: 10px;
    margin-right: -30px
}

.truncate {
    max-width: 230px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.tab-list-title {
    font-size: 1.5em;
    padding-left: 10px;
}

.tab-list-group-item {
    padding-top: 5px;
    padding-bottom: 5px;
}

.tab-list-group-item-label {
    padding-top: 5px;
    margin-bottom: 0;
    font-weight: bold;
    font-style: italic
}

.filterHelp {
    font-size: 1.5em;
    color: #00A38D;
    border-radius: 50%;
    margin-left: 18px;
    margin-bottom: 5px;
}

.filter-form {
    width: 230px;
    padding-bottom: 10px;
    padding-right: 10px;
}

.selected {
    background-color: #cfe6e1 !important;
}

</style>

<i18n>
en:
    title: Data management
    list: List
    no-list: "no {itemType} found"
    filters: Filters
    filter-help: " Site filters are linked to the experiments they host"

fr:
    title: Gestion des données
    list: Liste
    no-list: "pas de {itemType} trouvé"
    filters: Filtres
    filter-help: "Les filtres des sites sont liés aux expérimentations qu'ils accueillent"

</i18n>