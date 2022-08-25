<template>
    <ValidationObserver>
        <opensilex-ExternalReferencesForm
                :references.sync="form"
                :displayInsertButton="false"
                :ontologiesToSelect="selectedOntologies"
        ></opensilex-ExternalReferencesForm>
    </ValidationObserver>
</template>

<script lang="ts">

    import {Component, PropSync} from "vue-property-decorator";
    import Vue from "vue";
    import DimensionCreate from "./DimensionCreate.vue";
    // @ts-ignore
    import { DimensionCreationDTO } from "opensilex-core/index";

    @Component
    export default class DimensionExternalReferencesForm extends Vue {

        selectedOntologies: string[] = DimensionCreate.selectedOntologies;

        @PropSync("form")
        dto: DimensionCreationDTO;

    }
</script>

<style scoped>

</style>