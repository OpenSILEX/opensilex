<template>
  <div>
    <nav v-if="user.isLoggedIn()" v-bind:class="{'hide-responsive':!menuVisible}">
      <ul>
        <li>
          <router-link to="/">Home</router-link>
        </li>
        <li v-for="item in menu" v-bind:key="item.id">
          <span v-if="!item.route">{{item.label}}</span>
          <router-link v-else :to="item.route.path">{{item.label}}</router-link>

          <ul v-if="item.children.length > 0">
            <li v-for="itemChild in item.children" v-bind:key="itemChild.id">
              <span v-if="!itemChild.route">{{itemChild.label}}</span>
              <router-link v-else :to="itemChild.route.path">{{itemChild.label}}</router-link>
            </li>
          </ul>
        </li>
      </ul>
    </nav>
    <div class="hamburger" v-if="user.isLoggedIn()">
      <input type="checkbox" v-model="menuVisible" />
      <span class="hamburger1"></span>
      <span class="hamburger2"></span>
      <span class="hamburger3"></span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import { MenuItemDTO } from "../../lib";

@Component
export default class DefaultMenuComponent extends Vue {
  get menu(): Array<MenuItemDTO> {
    return this.$store.state.menu;
  }

  get user() {
    return this.$store.state.user;
  }

  private menuVisible: boolean = false;
}
</script>

<style scoped lang="scss">
@import "../../../styles/styles";

ul {
  list-style-type: none;
  padding: 0;
}

ul > li > ul {
  margin-left: 15px;
}

nav {
  padding: 15px;
  min-height: 100%;
  background-color: getVar(--highlightBackgroundColorDark);
  color: getVar(--defaultColorLight);
}

/**
 * Responsive hamburger hide/show menu
 */
.hamburger {
  display: none;
  position: absolute;
  right: 7%;
  top: 31px;
  z-index: 1;
  -webkit-user-select: none;
  user-select: none;
}

.hamburger input {
  display: block;
  width: 40px;
  height: 32px;
  position: absolute;
  top: -16px;
  left: -9px;
  cursor: pointer;
  opacity: 0;
  z-index: 2;

  -webkit-touch-callout: none;
}

.hamburger span {
  display: block;
  width: 33px;
  height: 4px;
  margin-bottom: 5px;
  position: relative;

  background: getVar(--linkColor);
  border-radius: 3px;
  border-style: solid;
  border-width: 2px;
  z-index: 1;

  transform-origin: 4px 0px;

  transition: transform 0.5s cubic-bezier(0.77, 0.2, 0.05, 1),
    opacity 0.55s ease;

  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  -webkit-background-clip: content-box;
  background-clip: content-box;
}

.hamburger span:first-child {
  transform-origin: 0% 0%;
}

.hamburger span:nth-last-child(2) {
  transform-origin: 0% 100%;
}

.hamburger1,
.hamburger2 {
  top: -6px;
}

.hamburger3 {
  top: -33px;
}

.hamburger input:checked ~ span {
  opacity: 1;
  transform: rotate(45deg) translate(-2px, -1px);
  background: getVar(--linkColor);
}

.hamburger input:checked ~ span:nth-last-child(3) {
  opacity: 0;
  transform: rotate(0deg) scale(0.2, 0.2);
}

.hamburger input:checked ~ span:nth-last-child(2) {
  transform: rotate(-45deg) translate(0, -1px);
}

.hamburger input:hover ~ span {
  background: getVar(--linkHighlightColor);
}

@media (max-width: 600px) {
  .hamburger {
    display: block;
  }

  nav.hide-responsive {
    display: none;
  }

  nav {
    z-index: 888;
    position: absolute;
    height: auto;
    min-height: 0;
    width: 100%;
    text-align: center;
  }

  ul > li > ul {
    margin-left: 0;
  }
}
</style>
