<template>
    <div class="container-fluid">
        <opensilex-PageHeader
                icon="fa#sun"
                title="component.menu.variables"
                description="VariableDetails.title"
        ></opensilex-PageHeader>
        <opensilex-NavBar returnTo="/variables">
            <template v-slot:linksLeft>
                <li class="active">
                    <b-button
                            class="mb-2 mr-2"
                            variant="outline-primary"
                    >{{$t('VariableDetails.title')}}
                    </b-button>
                </li>
            </template>
        </opensilex-NavBar>
        <div class="container-fluid">
            <b-row>
                <b-col>
                    <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-UriView :uri="variable.uri" :url="variable.uri"></opensilex-UriView>
                            <opensilex-StringView label="component.common.name" :value="variable.name"></opensilex-StringView>
                            <opensilex-StringView label="VariableForm.longName" :value="variable.longName"></opensilex-StringView>
                            <opensilex-TextView label="component.common.description" :value="variable.comment"></opensilex-TextView>
                        </template>
                    </opensilex-Card>
                </b-col>
                <b-col>
                    <opensilex-Card label="VariableDetails.structure" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-UriView title="Variables.entity"  v-if="variable.entity"
                                               :value="variable.entity.name" :uri="variable.entity.uri" :url="variable.entity.uri"></opensilex-UriView>
                            <opensilex-UriView title="Variables.quality"  v-if="variable.quality"
                                               :value="variable.quality.name" :uri="variable.quality.uri" :url="variable.quality.uri"></opensilex-UriView>
                            <opensilex-UriView title="Variables.method"  v-if="variable.method"
                                               :value="variable.method.name" :uri="variable.method.uri" :url="variable.method.uri"></opensilex-UriView>
                            <opensilex-UriView title="Variables.unit"  v-if="variable.unit"
                                               :value="variable.unit.name" :uri="variable.unit.uri" :url="variable.unit.uri"></opensilex-UriView>
                        </template>
                    </opensilex-Card>
                </b-col>
            </b-row>
            <b-row>
                <b-col>
                    <opensilex-Card label="VariableDetails.advanced" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-StringView label="VariableForm.dimension" :value="variable.dimension"></opensilex-StringView>
                            <opensilex-StringView label="VariableForm.synonym" :value="variable.synonym"></opensilex-StringView>
                            <opensilex-UriView title="VariableForm.trait-uri"
                                               :uri="variable.traitUri" :url="variable.traitUri"></opensilex-UriView>
                            <opensilex-StringView label="VariableForm.trait-name" :value="variable.traitName"></opensilex-StringView>
                        </template>
                    </opensilex-Card>
                </b-col>
                <b-col>
                    <opensilex-Card label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-ExternalReferencesDetails
                                    :skosReferences="variable"></opensilex-ExternalReferencesDetails>
                        </template>
                    </opensilex-Card>
                </b-col>
            </b-row>
        </div>
    </div>
</template>

<script lang="ts">
    import {Component, Prop, PropSync} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
    import {VariablesService} from "opensilex-core/api/variables.service";
    import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";

    @Component
    export default class VariableDetails extends Vue {
        $opensilex: any;
        $store: any;
        $route: any;
        $t: any;
        $i18n: any;
        service: VariablesService;

        get user() {
            return this.$store.state.user;
        }

        variable: VariableDetailsDTO = {
            exactMatch: [],
            closeMatch: [],
            broader: [],
            narrower: []
        };

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
            this.loadVariable(this.$route.params.uri);
        }

        loadVariable(uri: string) {
            this.service.getVariable(uri)
                .then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
                    this.variable = http.response.result;
                })
                .catch(this.$opensilex.errorHandler);
        }
    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    VariableDetails:
        title: Detailled variable view
        entity-name: Entity name
        quality-name: Quality name
        method-name: Method name
        unit-name: Unit name
        structure: Structure
        advanced: Advanced informations
fr:
    VariableDetails:
        title: Vue détaillée de la variable
        entity-name: Nom d'entité
        quality-name: Nom de qualité
        method-name: Nom de méthode
        unit-name: Nom d'unité
        structure: Structure
        advanced: Informations avancées
</i18n>
