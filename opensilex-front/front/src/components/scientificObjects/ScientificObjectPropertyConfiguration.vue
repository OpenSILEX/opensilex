<template>

    <div class="mr-4 properties-configuration">  
        <b-dropdown variant="link" ref="propertiesConfiguration" right class="right">
            <template v-slot:button-content>
                <span class="dropdown-toggle btn btn-icon btn-primary">
                    <i class="icon ik ik-sliders"></i>
                </span>
            </template>

            <b-dropdown-header class="properties-dropdown-header">
                <span>{{ $t('component.scientificObjects.search.propetiesConfiguration') }}</span>
            </b-dropdown-header>

            <b-dropdown-text :key="index" v-for="(property, index) in copyOfScientificObjectPropertyTypeConfiguration">
                <label class="custom-control custom-checkbox m-0">
                    <input type="checkbox" class="custom-control-input" :value="true" v-model="property.show">
                    <span class="custom-control-label">{{ $t(property.name) }}</span>
                </label>
            </b-dropdown-text>

            <b-dropdown-text class="properties-dropdown-footer">
                <button type="button" class="btn btn-outline-secondary" v-on:click="closePropertiesConfiguration(false)">{{ $t('component.common.cancel') }}</button>
                <button type="button" class="btn btn-success" v-on:click="closePropertiesConfiguration(true)">{{ $t('component.common.validate') }}</button>
            </b-dropdown-text>
            
        </b-dropdown>

    </div>

</template>

<script lang="ts">

    import { Component, Prop } from "vue-property-decorator";
    import Vue from "vue";
    import { BDropdown } from 'bootstrap-vue';

    @Component
    export default class ScientificObjectPropertyConfiguration extends Vue {

        @Prop()
        scientificObjectPropertyTypeConfiguration;

        copyOfScientificObjectPropertyTypeConfiguration = [];

        created() {
            this.copyOfScientificObjectPropertyTypeConfiguration = JSON.parse(JSON.stringify(this.scientificObjectPropertyTypeConfiguration));
        }

        closePropertiesConfiguration(validate: boolean) {
            let dropdown = this.$refs.propertiesConfiguration as BDropdown;
            dropdown.hide();
            if(validate) {
                for(let i=0; i<this.scientificObjectPropertyTypeConfiguration.length; i++) {
                    this.scientificObjectPropertyTypeConfiguration[i].show = this.copyOfScientificObjectPropertyTypeConfiguration[i].show;
                }
            } else {
                this.copyOfScientificObjectPropertyTypeConfiguration = JSON.parse(JSON.stringify(this.scientificObjectPropertyTypeConfiguration));
            }
        }

    }

</script>

<style scoped lang="scss">

    .right {
        float: right;
    }

</style>

