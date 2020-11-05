<template>
    <div class="container-fluid">
        <opensilex-PageHeader
            icon="fa#sun"
            description="VariableView.type"
            :title="variable.name"
        ></opensilex-PageHeader>

        <opensilex-PageActions :tabs="true" :returnButton="true">
            <template v-slot>
                <b-nav-item
                    class="ml-3"
                    :active="isDetailsTab()"
                    :to="{ path: '/variable/details/' + encodeURIComponent(variable.uri) }"
                >{{ $t("component.factor.details.label") }}</b-nav-item
                >

            </template>
        </opensilex-PageActions>
        <opensilex-PageContent>
            <template v-slot>
                <opensilex-VariableDetails
                    v-if="isDetailsTab()"
                    :variable="variable"
                    @onUpdate="updateVariable($event)"
                ></opensilex-VariableDetails>
            </template>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
import { Component} from "vue-property-decorator";
import Vue from "vue";
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import VariableForm from "../form/VariableForm.vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class VariableView extends Vue {

    $opensilex: OpenSilexVuePlugin;
    service: VariablesService;
    $store: any;
    $route: any;
    $router: any;

    $t: any;
    $i18n: any;

    variable : VariableDetailsDTO = VariableForm.getEmptyForm();

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    created() {
        this.service = this.$opensilex.getService("opensilex.VariablesService");
        this.loadVariable(decodeURIComponent(this.$route.params.uri));
    }

    isDetailsTab() {
        return this.$route.path.startsWith("/variable/details/");
    }

    loadVariable(uri: string) {
        this.service.getVariable(uri).then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
            this.variable = http.response.result;
        }).catch(this.$opensilex.errorHandler);
    }

    updateVariable(variable){
        this.variable = variable;
    }

}
</script>

<style scoped lang="scss">
</style>

