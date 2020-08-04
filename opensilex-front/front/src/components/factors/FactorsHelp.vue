<template>
  <div v-if="(lang === 'fr')">
    <h1 class="code-line" data-line-start="0" data-line-end="1">
      <a id="Vocabulaire_pour_parler_des_facteurs_0"></a>Vocabulaire pour parler des facteurs
    </h1>
    <p
      class="has-line-data"
      data-line-start="2"
      data-line-end="3"
    >Auteur: Jean-Eudes Hollebecq - Opensilex</p>
    <h2 class="code-line" data-line-start="4" data-line-end="5">
      <a id="Plan_dexperiences_4"></a>Plan d’experiences
    </h2>
    <p
      class="has-line-data"
      data-line-start="6"
      data-line-end="7"
    >Avant-propos: les plans d’expériences, les variables mesurées et les facteurs envisagés ne sont pas toujours réalisés correctements, conçus sans interactions avec l’analyste statisticien.</p>
    <ol>
      <li class="has-line-data" data-line-start="8" data-line-end="11">
        plan complet :
        <br />Tous les niveaux de tous les facteurs sont représentés.
        <br />Ex: Facteur1 à 2 niveaux et facteur2 à 5 niveaux 2x5 = 10 combinaisons.
      </li>
      <li class="has-line-data" data-line-start="11" data-line-end="12">plan factoriel = idem</li>
      <li class="has-line-data" data-line-start="12" data-line-end="14">
        plan non croisé :
        <br />contraire d’un plan croisé, certaines combinaisons ne sont pas observées
      </li>
      <li class="has-line-data" data-line-start="14" data-line-end="16">
        plan équilibré :
        <br />même nombre d’individus dans chaque classe/ chaque cellule du plan
      </li>
      <li class="has-line-data" data-line-start="16" data-line-end="19">
        plan déséquilibré :
        <br />nombre d’individus différents dans chaque classe
      </li>
    </ol>
    <h3 class="code-line" data-line-start="19" data-line-end="20">
      <a id="Consquence_en_anova_19"></a>Conséquence en anova
    </h3>
    <p
      class="has-line-data"
      data-line-start="21"
      data-line-end="22"
    >Dans un plan non complet ou déséquilibré :</p>
    <ul>
      <li class="has-line-data" data-line-start="23" data-line-end="25">
        <p
          class="has-line-data"
          data-line-start="23"
          data-line-end="24"
        >Anova de TYPE I sera différente selon l’ordre d’apparition des facteurs.</p>
      </li>
      <li class="has-line-data" data-line-start="25" data-line-end="27">
        <p
          class="has-line-data"
          data-line-start="25"
          data-line-end="26"
        >Anova de TYPE III, qui qui correspondent à la variabilité expliquée par l’apport d’un effet sachant que tous les autres effets sont déjà présents dans le modèle.</p>
      </li>
    </ul>
    <p
      class="has-line-data"
      data-line-start="27"
      data-line-end="28"
    >Type I et Type III sont équivalentes dans un plan complet-équilibré</p>
    <h2 class="code-line" data-line-start="29" data-line-end="30">
      <a id="Facteurs_explicatifs_29"></a>Facteurs explicatifs
    </h2>
    <ol>
      <li class="has-line-data" data-line-start="31" data-line-end="34">
        <p class="has-line-data" data-line-start="31" data-line-end="33">
          Facteur à effet fixes :
          <br />Tous les niveaux du facteur sont représentatifs de la variabilité du facteur. Ce sont les niveaux d’intérêt
        </p>
      </li>
      <li class="has-line-data" data-line-start="34" data-line-end="36">
        <p class="has-line-data" data-line-start="34" data-line-end="36">
          Facteur à effets aléatoires :
          <br />Les niveaux du facteur ne sont pas représentatifs de l’ensemble des niveaux possible. Il s’agit d’un échantillon tiré au hasard parmi les différents niveaux possibles.
        </p>
      </li>
      <li class="has-line-data" data-line-start="36" data-line-end="39">
        <p class="has-line-data" data-line-start="36" data-line-end="38">
          Facteurs hirarchiques
          <br />l’interprétation d’un facteur (subordonné) dépends de la valeur prise par un autre facteur. La mesure du facteur (subordonné), se fait conditionellement aux valeurs prises par un second facteur.
        </p>
      </li>
    </ol>
    <pre><code
  class="language-r"
  data-line-start="40"
  data-line-end="44"
>Factors can either be fixed or random.
A factor is fixed when the levels under study are the only levels of interest.  
A factor is random when the levels under study are a random sample from a larger population   and the goal of the study is to make a statement regarding the larger population.
</code></pre>
    <p class="has-line-data" data-line-start="45" data-line-end="49">
      Remarque:
      <br />Selon l’objectif l’expérience le même facteur sera fixe ou aléatoire.
      <br />Ex : Une expérience qui vise à déterminer qui est le meilleur boulanger va considérer un effet “boulanger” (chaque participant) comme fixe.
      <br />Tandis que si on cherche à savoir si l’effet du boulanger (en général) est important dans la qualité du pain, on considère l’effet “boulanger” comme aléatoire (on ne peut pas tester tous les boulangers existants).
    </p>
    <p
      class="has-line-data"
      data-line-start="50"
      data-line-end="51"
    >Lors de modèle mixte avec tous les effets et interactions possibles, on retire les interactions d’ordre le plus élevé en premier pour aboutir à un modèle interprétable.</p>
  </div>
  <div v-else>
    <h1 class="code-line" data-line-start="0" data-line-end="1">
      <a id="Vocabulary_concerning_factors_0"></a>Vocabulary concerning factors
    </h1>
    <p
      class="has-line-data"
      data-line-start="2"
      data-line-end="3"
    >Author: Jean-Eudes Hollebecq - Opensilex</p>
    <h2 class="code-line" data-line-start="4" data-line-end="5">
      <a id="Experimental_design_4"></a>Experimental design
    </h2>
    <p
      class="has-line-data"
      data-line-start="6"
      data-line-end="7"
    >Foreword: Some experimental designs, the variables measured, the factors mentionned are not designed properly, made without the interaction of the statistician, making the analysis more difficult.</p>
    <ol>
      <li class="has-line-data" data-line-start="8" data-line-end="11">
        crossed design :
        <br />All the factors levels are represented
        <br />Ex: Factor1 with 2 levels and factor2 with 5 levels 2x5 = 10 combinaisons.
      </li>
      <li class="has-line-data" data-line-start="11" data-line-end="12">factorial design = idem</li>
      <li class="has-line-data" data-line-start="12" data-line-end="14">
        non-crossed design :
        <br />not a complete design, some combinaisons are not represented.
      </li>
      <li class="has-line-data" data-line-start="14" data-line-end="16">
        balanced design :
        <br />same number of individuals for each combinaison of levels.
      </li>
      <li class="has-line-data" data-line-start="16" data-line-end="19">
        unbalanced design :
        <br />uneven number of individuals for each level.
      </li>
    </ol>
    <h3 class="code-line" data-line-start="19" data-line-end="20">
      <a id="Analysis_of_variance_19"></a>Analysis of variance
    </h3>
    <p
      class="has-line-data"
      data-line-start="21"
      data-line-end="22"
    >For a non-crossed or unbalanced design :</p>
    <ul>
      <li class="has-line-data" data-line-start="23" data-line-end="25">
        <p
          class="has-line-data"
          data-line-start="23"
          data-line-end="24"
        >Anova TYPE I will be different according to the order of appearance of the levels</p>
      </li>
      <li class="has-line-data" data-line-start="25" data-line-end="27">
        <p
          class="has-line-data"
          data-line-start="25"
          data-line-end="26"
        >Anova TYPE III, correspond to the variability of an effect knowing that all the other effects are already present in the model</p>
      </li>
    </ul>
    <p
      class="has-line-data"
      data-line-start="27"
      data-line-end="28"
    >Type I et Type III are equivalent in complete and balanced plan.</p>
    <h2 class="code-line" data-line-start="29" data-line-end="30">
      <a id="Explaining_factors_29"></a>Explaining factors
    </h2>
    <ol>
      <li class="has-line-data" data-line-start="31" data-line-end="33">
        Fixed effect factor :
        <br />All the levels are representative of the variability of the factor, they are the levels of interest.
      </li>
      <li class="has-line-data" data-line-start="33" data-line-end="35">
        Random effect factor :
        <br />The levels of the factor are not representative of all the variability of the factor, they are a sub-part of a larger population of different factor levels.
      </li>
      <li class="has-line-data" data-line-start="35" data-line-end="38">
        Nested factor
        <br />Interpretation of a factor (subordonate) is conditionned by the value taken by another factor (the superior)
      </li>
    </ol>
    <pre><code
  class="language-r"
  data-line-start="39"
  data-line-end="43"
>Factors can either be fixed or random.
A factor is fixed when the levels under study are the only levels of interest.  
A factor is random when the levels under study are a random sample from a larger population   and the goal of the study is to make a statement regarding the larger population.
</code></pre>
    <p class="has-line-data" data-line-start="44" data-line-end="48">
      <em>Nota bene</em>:
      <br />According to the objective of the experiment, the factor can be random or fixed.
      <br />Ex : An experiment that tend to discriminate the best baker will handle the “baker” effect (each participant) as a fixed effect.
      <br />Meanwhile if the objective is to evaluate wether the effect of the baker (in general) is important for the quality of the bread, then the “baker” effect will be random, as it is impossible to evaluate all the different bakers.
    </p>
    <p
      class="has-line-data"
      data-line-start="49"
      data-line-end="50"
    >When building a model with interactions between factors, it is important to delete the non-significative intertaction effects according to their complexity order (AnB &lt; AnBnC).</p>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FactorsHelp extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  $router: any;

  get lang() {
    console.log(this.$store.getters.language);
    return this.$store.getters.language;
  }

  selectedFactor: any = {
    uri: null,
    name: null,
    category: null,
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
  };

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
}
</script>

<style scoped lang="scss">
</style>

