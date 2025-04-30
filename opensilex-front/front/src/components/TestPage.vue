<template>
  <div class="testComponentContainer">
    <header>
      <opensilex-DataTable />
      <h1>
        Voici mon composant test 
        <span class="media">🐶</span>
      </h1>
      <h3> 
        <button class="btn logout" @click="logout">Logout</button>
        <button class="btn super" @click="redirectToSuper">Vers Un Super Composant 🦖</button>
        <button class="btn dash" @click="redirectToDash">Vers Un Dashboard Extraordinaire 🙈</button>
      </h3>
      <highcharts :options="chartOptions" />
    </header>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed, ref } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import DataTable from '../components/home/DataTable.vue';
import Highcharts from 'highcharts';
import HighchartsVue from 'highcharts-vue';

export default defineComponent({
  name: "TestPage",
  setup() {
    const store = useStore();
    const router = useRouter();

    const isLoggedIn = computed(() => store.state.user.loggedIn);

    const logout = () => {
      console.log("logout");
      store.commit("logout");
      store.commit("refresh");
    };

    const redirectToSuper = () => {
      router.push('/super'); 
    };

    const redirectToDash = () => {
      router.push('/dash'); 
    };

    const chartOptions = ref({
      title: {
        text: 'Exemple de graphique'
      },
      exporting: { enabled: true },
      series: [{
        data: [1, 2, 3, 4, 5]
      }]
    });

    return {
      logout, 
      redirectToSuper,
      redirectToDash,
      chartOptions
    };
  }
});
</script>

<style scoped>
.testComponentContainer {
  width: 100vw;
  height: 100vh;
  background: rgb(12,26,115);
  background: linear-gradient(
    183deg, rgba(12,26,115,0.7035189075630253) 16%,
    rgba(126,97,215,0.700717787114846) 40%,
    rgba(117,103,222,0.6951155462184874) 61%, 
    rgba(69,46,128,0.7035189075630253) 85%
  );
}
header {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-image: linear-gradient(to right, #8bc0c2 0%, #8891cc 100%) 1;
  border-width: 4px;
  border-style: solid;
  padding: 5px;
  
  min-width: 500px;
  padding: 20px;
  border-radius: 15px;
  background: linear-gradient(135deg, #6e8efb, #a777e3);
  color: white;
  box-shadow: 0px 10px 30px rgba(0, 0, 0, 0.2);
  text-align: center;
}

h1 {
  font-size: 24px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media {
  display: inline-block;
  margin-left: 10px;
  font-size: 30px;
  animation: rotateEmoji 3s linear infinite;
}

@keyframes rotateEmoji {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

h3 {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.btn {
  padding: 10px 20px;
  border: none;
  font-size: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease-in-out;
}

.logout {
  background: #ff4b5c;
  color: white;
}

.super {
  background: #4caf50;
  color: white;
}

.dash {
  background: #4ca0af;
  color: white;
}

.btn:hover {
  transform: scale(1.1);
}
</style>

