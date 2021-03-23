<template>
    <span>
        <span v-for="(property, index) in properties" :key="index" :title="getTitle(property.rdfType)" class="keyword badge badge-pill" v-bind:class="getRDFType(property.rdfType)">{{property.value}}</span>
    </span>

</template>

<script lang="ts">

    import { Component, Prop } from "vue-property-decorator";
    import Vue from "vue";
    import Oeso from "../../ontologies/Oeso";
    import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

    @Component
    export default class ScientificObjectPropertyList extends Vue {

        @Prop()
        properties;
        
        @Prop()
        scientificObjectPropertyTypeConfiguration;

        copyOfScientificObjectPropertyTypeConfiguration = {};
        
        getTitle(rdfType) {
            let property = this.scientificObjectPropertyTypeConfiguration.filter(value => value.uri == rdfType);
            if(property && property.length > 0) {
                return property[0].title;
            }
            return null;
        }

        getRDFType(rdfType) {
            let property = this.scientificObjectPropertyTypeConfiguration.filter(value => value.uri == rdfType);
            if(property && property.length > 0) {
                if(property[0].show) {
                    return property[0].class;
                }
            }
            return "hide";
        }

    }

</script>

<style scoped lang="scss">

    .hide {
        display: none;
    }

</style>
