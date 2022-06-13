<template>
    <opensilex-OntologyTypesView
        :rdfType="this.$opensilex.Oeev.getShortURI(this.$opensilex.Oeev.EVENT_TYPE_URI)"
        icon="ik#ik-settings"
        typeTitle="EventTypes.typeTitle"
        typeDescription="EventTypes.typeDescription"
        typeURI="/event-types"
        propertiesTitle="EventTypes.propertiesTitle"
        propertiesDescription="EventTypes.propertiesDescription"
        propertiesURI="/event-types/properties"
        :withProperties="true"
    ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class EventTypes extends Vue {
    $opensilex: OpenSilexVuePlugin;
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    EventTypes:
        typeTitle: Event types
        propertiesTitle: Event properties
        typeDescription: Manage and configure event types
        propertiesDescription: Manage and configure properties that can apply specifically to an event
fr:
    EventTypes:
        typeTitle: Types d'évenements
        propertiesTitle: Propriétés des évenements
        typeDescription: "Gérer et configurer les types d'évenements"
        propertiesDescription: "Gérer et configurer les propriétés qui peuvent s'appliquer spécifiquement à un évenement"
</i18n>


