<template>
    <b-card v-if="selected && selected.uri">
        <template v-slot:header>
            <h3>
                <opensilex-Icon icon="ik#ik-clipboard" />
                {{$t("component.common.details-label")}}
            </h3>
        </template>
        <div>
            <!-- URI -->
            <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
            <!-- Name -->
            <opensilex-TextView label="component.common.name" :value="selected.name"></opensilex-TextView>
            <!-- Description -->
            <opensilex-TextView :value="selected.description" label="component.common.description"></opensilex-TextView>
            <!-- symbol -->
            <opensilex-TextView label="AgroportalUnitForm.symbol" :value="selected.symbol"></opensilex-TextView>
            <!-- alternative symbol -->
            <opensilex-TextView label="AgroportalUnitForm.alternative-symbol" :value="selected.alternative_symbol"></opensilex-TextView>
            <!-- Metadata -->
            <opensilex-MetadataView
                v-if="selected.publisher && selected.publisher.uri"
                :publisher="selected.publisher"
                :publicationDate="selected.publication_date"
                :lastUpdatedDate="selected.last_updated_date" 
            ></opensilex-MetadataView>
        </div>
    </b-card>
</template>

<script lang="ts">
import { Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import { UnitDetailsDTO } from 'opensilex-core/index';


@Component
export default class UnitDetails extends Vue {
    $opensilex: any;

    @Prop()
    selected : UnitDetailsDTO;
}
</script>

<style scoped lang="scss">
</style>
