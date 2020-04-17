<template>
  <div class="page-header">
    <div class="row align-items-end">
      <div class="col-lg-8">
        <div class="page-header-title">
          <i class="ik bg-phis-green" :class="icon"></i>
          <div class="d-inline">
            <h5>{{ $t(title) }}</h5>
            <span>{{ $t(description) }}</span>
          </div>
        </div>
      </div>
      <div class="col-lg-4">
        <nav class="breadcrumb-container" aria-label="breadcrumb">
          <ol class="breadcrumb">
            <li class="breadcrumb-item">
              <router-link :to="{path: '/dashboard'}" :title="$t('component.menu.backToDashboard')">
                <i class="ik ik-grid mr-1"></i>
                {{ $t('component.menu.dashboard') }}
              </router-link>
            </li>
            <li class="breadcrumb-item active">
              <i class="ik mr-1" :class="icon"></i>
              {{ $t(title) }}
            </li>
          </ol>
        </nav>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class PageHeader extends Vue {
  $opensilex: any;

  @Prop()
  icon;

  @Prop()
  title;

  @Prop()
  description;

}
</script>

<style scoped lang="scss">
</style>