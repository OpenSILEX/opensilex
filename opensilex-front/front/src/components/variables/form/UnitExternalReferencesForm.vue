<template>
    <ValidationObserver ref="unitValidatorRef">
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
    import UnitCreate from "./UnitCreate.vue";
    // @ts-ignore
    import { UnitCreationDTO } from "opensilex-core/index";

    @Component
    export default class UnitExternalReferencesForm extends Vue {

        selectedOntologies: string[] = UnitCreate.selectedOntologies;

        @PropSync("form")
        dto: UnitCreationDTO;

    }
</script>

<style scoped>

</style>