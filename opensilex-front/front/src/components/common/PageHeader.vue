<template>
  <div class="page-header">
    <div class="row">
      <div class="col-lg-8">
        <div class="page-header-title">
          <div class="d-inline">
            <h5>
              <i class="ik" :class="icon"></i>
              &nbsp;{{ $t(title) }}
            </h5>
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
.page-header-title i {
  float: left;
  width: 40px;
  height: 40px;
  border-radius: 5px;
  margin-right: 20px;
  vertical-align: middle;
  font-size: 22px;
  color: #fff;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 12px -3px rgba(0, 0, 0, 0.5);
  background-color: #00a38d;
}

h5 {
  margin-bottom: 0;
}
</style>