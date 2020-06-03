<template>

    <div class="card-vertical-group">
        
        <div class="card">
            
            <div v-if="showTitle" class="card-header">
                <h3 class="mr-3"><i class="ik ik-search"></i>{{ $t(labelTitle) }}</h3>
            </div>

            <div class="card-body row">
                <slot name="standardSearch"></slot>
            </div>

            <div class="card" v-if="showAdvancedSearch">
                <div class="card-header sub-header">
                    <h3 class="mr-3">{{ $t('component.scientificObjects.search.advancedSearch') }}</h3>
                    <div class="card-header-right">
                        <div class="card-option">
                            <li><i class="ik minimize-card ik-plus" v-on:click="toogleAdvancedSearch($event)"></i></li>
                        </div>
                    </div>
                </div>
                <div class="card-body advancedSearch" style="background-color: rgb(246, 248, 251);" v-bind:class="{ 'open': advancedSearchOpen}">
                    <slot name="advancedSearch"></slot>
                </div>
            </div>
        
        </div>

        <div class="card">
            <div class="card-footer text-right">
                <button type="button" class="btn btn-light" v-on:click="resetMethod"><i class="ik ik-x"></i>{{$t(labelReset)}}</button>
                <button type="button" class="btn btn-primary" v-on:click="searchMethod"><i class="ik ik-search"></i>{{$t(labelSearch)}}</button>
            </div>
        </div>

    </div>

</template>

<script lang="ts">

import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class SearchForm extends Vue {
    
    $opensilex: any;

    @Prop()
    labelTitle;

    @Prop({ 
        default: "component.common.search.clear-button" 
    })
    labelReset;

    @Prop({ 
        default: "component.common.search.search-button"
    })
    labelSearch;

    @Prop({ 
        default: false 
    })
    showAdvancedSearch;

    @Prop({ 
        default: true 
    })
    showTitle;

    @Prop({
        type: Function
    })
    searchMethod: Function;

    @Prop({
        type: Function
    })
    resetMethod: Function;

    advancedSearchOpen = false;

    toogleAdvancedSearch() {
        this.advancedSearchOpen = !this.advancedSearchOpen;
    }

}
</script>

<style scoped lang="scss">
</style>
