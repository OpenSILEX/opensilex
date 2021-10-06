<template>
    <opensilex-SelectForm
        id="scientificObjectSelector"
        :label="label"
        :placeholder="placeholder"
        noResultsText="ScientificObjectSelector.no-results-text"
        :selected.sync="scientificObjectURIs"
        :multiple="multiple"
        :required="required"
        :searchMethod="search"
        :itemLoadingMethod="load"
        :conversionMethod="dtoToSelectNode"
        :key="lang"
        :showCount="true"
    ></opensilex-SelectForm>
</template>

<script lang="ts">

import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import {
    ScientificObjectsService
} from "opensilex-core/index";
import {ScientificObjectNodeDTO} from "opensilex-core/model/scientificObjectNodeDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

@Component
export default class ScientificObjectSelector extends Vue {

    $opensilex: any;
    $service: ScientificObjectsService;
    $store: any;

    @PropSync("value")
    scientificObjectURIs;

    @Prop({default: false})
    multiple;

    @Prop({default: false})
    required;

    @Prop({default: "component.menu.scientificObjects"})
    label;

    @Prop({default: "ScientificObjectSelector.placeholder"})
    placeholder;

    dtoByUriCache: Map<string,ScientificObjectNodeDTO>;

    created() {
        this.$service = this.$opensilex.getService("opensilex.ScientificObjectsService");
        this.dtoByUriCache = new Map();
    }

    search(query, page, pageSize) {
        return this.$service
            .searchScientificObjects(
                undefined, // experiment uri?: string,
                undefined, // rdfTypes?: Array<string>,
                query, // pattern?: string,
                undefined, // parentURI?: string,
                undefined, // Germplasm
                undefined, // factorLevels?: Array<string>,*
                undefined, // facility?: string,
                undefined,
                undefined,
                ["name=asc"], // orderBy?: ,
                page, // page?: number,
                pageSize // pageSize?: number
            ).then((http : HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {

                if (http && http.response) {
                    this.dtoByUriCache.clear();
                    http.response.result.forEach(dto => {
                        this.dtoByUriCache.set(dto.uri,dto);
                    })
                }
                return http;

            }).catch(this.$opensilex.errorHandler);
    }



    load(scientificObjects) {

        if (!scientificObjects){
            return undefined;
        }

        if(! Array.isArray(scientificObjects)){
            scientificObjects = [scientificObjects];
        }else if(scientificObjects.length == 0){
            return undefined;
        }                                                                       

        let dtosToReturn = [];

        if (this.dtoByUriCache.size == 0) {
            scientificObjects.forEach(scientificObject => {

                // if the scientificObject is an object (and not an uri) with an already filled name and uri, then no need to call service
                if (scientificObject.name && scientificObject.name.length > 0 && scientificObject.uri && scientificObject.uri.length > 0) {
                    dtosToReturn.push(scientificObject);
                }
            })

            // if all element to load are objects then just return them
            if (dtosToReturn.length == scientificObjects.length) {
                return dtosToReturn;
            }
        }else {

            // if object have already been loaded, then it's not needed to call the GET{uri} service just for retrieve the object name
            // since the name is returned by the SEARCH service and the result is cached into dtoByUriCache

            scientificObjects.forEach(scientificObject => {
                let loadedDto = this.dtoByUriCache.get(scientificObject);
                dtosToReturn.push(loadedDto);
            });

            return dtosToReturn;
        }

        return this.$service
            .getScientificObjectsListByUris(undefined,scientificObjects)
            .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
                return (http && http.response) ? http.response.result : undefined
        }).catch(this.$opensilex.errorHandler);

    }

    dtoToSelectNode(dto: ScientificObjectNodeDTO) {
        if (!dto) {
            return undefined;
        }
        return {label: dto.name, id: dto.uri};
    }

    get lang() {
        return this.$store.getters.language;
    }

    // select(value) {
    //     this.$emit("select", value);
    // }
    //
    // deselect(value) {
    //     this.$emit("deselect", value);
    // }

}
</script>

<i18n>

en:
    ScientificObjectSelector:
        placeholder: Search and select scientific objects
        no-results-text: No scientific object found
fr:
    ScientificObjectSelector:
        placeholder: Rechercher et sélectionner un ou plusieurs objets scientifiques
        no-results-text: Aucun objet scientifique trouvé

</i18n>