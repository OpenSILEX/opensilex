<template>
    <ValidationObserver ref="qualityValidatorRef">
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
    import {ExternalOntologies} from "../../../models/ExternalOntologies";
    import QualityCreate from "./QualityCreate.vue";
    import { QualityCreationDTO } from "opensilex-core/index";

    @Component
    export default class QualityExternalReferencesForm extends Vue {

        selectedOntologies: string[] = QualityCreate.selectedOntologies;

        @PropSync("form")
        dto: QualityCreationDTO;

    }
</script>

<style scoped>

</style>