<template>
  <div class="page-header">

      <opensilex-Icon :icon="icon" class="title-icon" />
      <div class="header-title">
    <h5>
      <span class="title-title"><slot name="title">&nbsp;{{ $t(title) }}</slot></span>
    </h5>
    </div>
    <div class="header-desc">
    <span class="title-description"><slot name="description" >{{ $t(description) }}</slot></span>
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

  @Prop({default: false})
  isExperimentalFeature: boolean;
}
</script>

<style scoped lang="scss">

.page-header{
  z-index: 1030;
}
.title-icon {
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
.header-title, .header-desc, .title-description {
  width: 80%;
}

.navbar-title {
  white-space: pre;
}

@media only screen and (min-width: 1201px) {
  .header-title {
    height: 28px;
    overflow: hidden;
  }
  .header-desc{
    height: 20px;
    overflow: hidden;
  }
}

@media (min-width: 676px) and (max-width: 1200px) {
  .header-title{
    width: 65%;
    height: 37px;
    overflow: hidden;
  }
    .header-desc{
    width: 65%;
    height: 20px;
    overflow: hidden;
  }

  .title-title {
    font-size: 0.8em;
    display: block;
    line-height: 1.2;
  }

}

@media (min-width: 200px) and (max-width: 675px) {
  .header-title{
    width: 100%;
    height: 35px;
    overflow: hidden;
    margin-left: -30px;
  }
  .header-desc{
    width: 100%;
    height: 20px;
    overflow: hidden;
    margin-left: -30px;
  }

  .title-icon{
    display: none;
  }

  .title-title {
    font-size: 0.7em;
    display: block;
    line-height: 1.2;
    font-weight: 600;
  }

  .title-title, .title-description {
    margin-left: 30px
  }
}
</style>