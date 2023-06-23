<template>
    <div>
        <div v-if="lang === 'fr'">
            <DataverseHelpFR></DataverseHelpFR>
        </div>
        <div v-else>
            <DataverseHelpEN></DataverseHelpEN>
        </div>
        <div class="DataverseHelpOkButton">
            <button
                type="button"
                class="btn greenThemeColor"
                v-on:click="hideBtnClick"
            >
                {{ $t('component.common.ok') }}
            </button>
        </div>
    </div>
</template>

<script lang="ts">
    import { Component, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import DataverseHelpFR from "./DataverseHelpFR.md";
    import DataverseHelpEN from "./DataverseHelpEN.md";
    import VueRouter, {Route} from "vue-router";
    import VueI18n from "vue-i18n";
    import {OpenSilexStore} from "../../../../../opensilex-front/front/src/models/Store";
    import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";

    Vue.component("DataverseHelpEN", DataverseHelpEN);
    Vue.component("DataverseHelpFR", DataverseHelpFR);

    @Component
    export default class DataversesHelp extends Vue {
        $opensilex: OpenSilexVuePlugin;
        $router: VueRouter;
        $route: Route;
        $store: OpenSilexStore;
        $t: typeof VueI18n.prototype.t;

        get lang() {
            console.debug("Active language : " + this.$store.getters.language);
            return this.$store.getters.language;
        }

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        hideBtnClick(){
            this.$emit("hideBtnIsClicked");
        }
    }
</script>

<style scoped lang="scss">
.DataverseHelpOkButton{
    float: right;
    height: 30px;
}
</style>

