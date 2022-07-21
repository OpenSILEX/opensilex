<template>
    <div>
        <div v-if="lang === 'fr'">
            <VariableHelpFR></VariableHelpFR>
        </div>
        <div v-else>
            <VariableHelpEN></VariableHelpEN>
        </div>
        <div class="VariableHelpOkButton">
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
    import VariableHelpFR from "./../doc/VariableHelpFR.md";
    import VariableHelpEN from "./../doc/VariableHelpEN.md";

    Vue.component("VariableHelpEN", VariableHelpEN);
    Vue.component("VariableHelpFR", VariableHelpFR);

    @Component
    export default class VariablesHelp extends Vue {
        $opensilex: any;
        $store: any;
        $t: any;
        $i18n: any;
        $router: any;

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
        source = new Date().toLocaleTimeString();

        hideBtnClick(){
            this.$emit("hideBtnIsClicked");
        }
    }
</script>

<style scoped lang="scss">
.VariableHelpOkButton{
    float: right;
    height: 30px;
}
</style>

