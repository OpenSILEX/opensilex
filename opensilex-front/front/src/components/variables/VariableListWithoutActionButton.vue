<template>
    <div>
        <opensilex-PageContent>
            <template>

                <opensilex-SearchFilterField @clear="reset()" @search="refresh()" :showAdvancedSearch="true">
                    <template v-slot:filters>

                        <opensilex-FilterField>
                            <label>{{ $t("ExperimentList.filter-label") }}</label>
                            <opensilex-StringFilter
                                :filter.sync="filter.name"
                                placeholder="VariableList.name-placeholder"
                            ></opensilex-StringFilter>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-EntitySelector
                                label="VariableView.entity"
                                :entity.sync="filter.entity"
                            ></opensilex-EntitySelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-CharacteristicSelector
                                label="VariableView.characteristic"
                                :characteristic.sync="filter.characteristic"
                            ></opensilex-CharacteristicSelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-GroupVariablesSelector
                                label="VariableView.groupVariable"
                                :variableGroup.sync="filter.group"
                            ></opensilex-GroupVariablesSelector>
                        </opensilex-FilterField>

                    </template>
                    <template v-slot:advancedSearch>

                        <opensilex-FilterField>
                            <opensilex-InterestEntitySelector
                                label="VariableForm.interestEntity-label"
                                :interestEntity.sync="filter.entityOfInterest"
                            ></opensilex-InterestEntitySelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-MethodSelector
                                label="VariableView.method"
                                :method.sync="filter.method"
                            ></opensilex-MethodSelector>
                        </opensilex-FilterField>

                        <opensilex-FilterField>
                            <opensilex-UnitSelector
                                label="VariableView.unit"
                                :unit.sync="filter.unit"
                            ></opensilex-UnitSelector>
                        </opensilex-FilterField>

                    </template>
                </opensilex-SearchFilterField>

                <opensilex-TableAsyncView
                    ref="tableRef"
                    :searchMethod="searchVariablesWithAttribute"
                    :fields="fields"
                    defaultSortBy="name"
                    :isSelectable="isSelectable"
                    :maximumSelectedRows="maximumSelectedRows"
                    labelNumberOfSelectedRow="VariableList.selected"
                    :iconNumberOfSelectedRow="iconNumberOfSelectedRow"
                    :defaultPageSize="pageSize">

                    <template v-slot:cell(name)="{data}">
                        <opensilex-UriLink
                            :uri="data.item.uri"
                            :value="data.item.name"
                            :url="'/variable/details/'+ encodeURIComponent(data.item.uri)"
                        ></opensilex-UriLink>
                    </template>
                    <template v-slot:row-details="{data}">
                        <div v-if="variableGroupsList[data.item.uri] && variableGroupsList[data.item.uri].length > 0">
                            <div>{{ $t("VariableList.variablesGroup") }}:</div>
                            <ul>
                                <li v-for="(vg, index) in variableGroupsList[data.item.uri]" :key="index">{{vg.name}}
                                </li>
                            </ul>
                        </div>
                        <div v-else> {{ $t("VariableList.not-used-in-variablesGroup") }}</div>
                    </template>
                    <template v-slot:cell(_entity_name)="{data}">{{ data.item.entity.name }}</template>
                    <template v-slot:cell(_interest_entity_name)="{data}">
                        {{ data.item.entity_of_interest ? data.item.entity_of_interest.name : "" }}
                    </template>
                    <template v-slot:cell(_characteristic_name)="{data}">{{ data.item.characteristic.name }}</template>
                    <template v-slot:cell(_method_name)="{data}">{{ data.item.method.name }}</template>
                    <template v-slot:cell(_unit_name)="{data}">{{data.item.unit.name }}</template>
                </opensilex-TableAsyncView>

            </template>
        </opensilex-PageContent>

    </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import VariableList from "./VariableList.vue";

@Component
export default class VariableListWithoutActionButton extends VariableList {

}
</script>

<style scoped lang="scss">
</style>