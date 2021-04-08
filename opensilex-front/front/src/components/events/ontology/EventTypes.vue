<template>
    <opensilex-OntologyTypesView
            :rdfClass="$opensilex.Oeev.EVENT_TYPE_URI"
            icon="ik#ik-settings"
            typeTitle="EventTypes.typeTitle"
            typeURI="/event-types"
            propertiesTitle="EventTypes.propertiesTitle"
            propertiesURI="/event-types/properties"
    ></opensilex-OntologyTypesView>
</template>

<script lang="ts">
    import { Component} from "vue-property-decorator";
    import Vue from "vue";

    @Component
    export default class EventTypes extends Vue {
        $opensilex: any;
    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    EventTypes:
        typeTitle: Event types
        propertiesTitle: Event properties
fr:
    EventTypes:
        typeTitle: Types d'évenements
        propertiesTitle: Propriétés des évenements
</i18n>


