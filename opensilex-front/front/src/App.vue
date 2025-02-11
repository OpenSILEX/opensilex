<template>
<h2>Composant App</h2>
<p>{{ $t('component.common.uri-help-message') }}</p>
</template>

<script lang="ts">
import { defineComponent } from 'vue'

export default defineComponent({})
</script>

<style lang="scss">
//@import "./styles/common.scss";

header {
  display: flex;
}

main {
  background-color: getVar(--defaultColorLight);
  color: getVar(--defaultColorDark);
}

#header-content {
  max-width: 1600px;
  margin: auto;
  display: flex;
  width: 100%;
}

#header-content .header-logo {
  width: 70%;
}

#header-content .header-login {
  width: 30%;
  text-align: right;
}

section#content-wrapper {
  display: flex;
  margin: 0 auto;
  height: 100%;
  flex-grow: 1;
  width: 100%;
}

main {
  padding: 15px;
  width: 100%;
}

.header-top.logged-out {
  box-shadow: none;
}

.wrapper.embed .page-wrap .main-content {
  margin-top: 0px;
  margin-left: 0px;
  padding: 15px;
}


.notificationMessageContainer{
  display: flex;
  position: relative;
  z-index: 1029; // header has an index of 1030, must be lower to allow the display of dropdown buttons above
  width: 100%;
  min-height: 35px;
  top: 65px;
  padding: 0 10px 0 10px;
  justify-content: center;
  align-items: center;
  font-family: sans-serif;
  font-size: small;
  font-style: oblique;
  font-weight: bold;
  color: darkslategray;
}

.notificationMessageDefaultColor {
  background: linear-gradient(45deg, #CFD8D5, #B1D8CB);
}
.notificationMessageInfoColor {
  background: linear-gradient(45deg, #c8e7eb, #91c8db);
}
.notificationMessageWarningColor {
    background: linear-gradient(45deg, #ebc8c8, #db9191);
}

.notificationWithMenu {
  left: 241px;
  max-width: calc(100% - 241px);
}

.notificationButtonContainer {
  position: inherit;
  right: 0;
}

.closeNotificationButton{
  border: none;
  padding: 1px 8px;
  margin: 4px;
  font-size: 1.5em !important;
  color:rgba(101, 101, 101, 0.5);
  font-weight: bolder;
  cursor: pointer;
  background: none;
}

.closeNotificationButton:hover{
  color : #00A28C;
  background: none;
}

.notificationText {
  padding-right: 25px;
  max-width: calc(100% - 50px);
}
</style>
