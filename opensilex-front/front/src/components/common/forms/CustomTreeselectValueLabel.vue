<template>
    <div>
        <slot name="value-label" v-bind:node="node">
            <div class="label" :title="node.label">{{ node.label }}</div>
        </slot>
    </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class CustomTreeselectValueLabel extends Vue {
  $opensilex: any;

}
</script>