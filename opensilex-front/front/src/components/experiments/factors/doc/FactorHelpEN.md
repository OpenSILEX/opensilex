# Vocabulary concerning factors

Author: Jean-Eudes Hollebecq - Opensilex

## Experimental design

Foreword: It is important to create the experimental with the implication of a statistician as early as possible. If variables, factors and the experimental design are properly made, then the subsequent analysis becomes easier and stronger.

1. Crossed design :  
   All the factors levels are represented
   Ex: Factor1 with 2 levels and factor2 with 5 levels; 2x5 = 10 combinations.
2. Factorial design = idem
3. Non-Crossed design :  
   not a complete design, some combinations are not represented.
4. Balanced design :  
   same number of individuals for each combination of levels.
5. Unbalanced design :  
   uneven number of individuals for each level.

The tool used to answer questions formulated like "has the effect of treatment X a significative impact over the production of grain ?", is the ANalysis Of VAriance (ANOVA).
This is the most common tool to answer statistical questions.

### Analysis of variance

Several types of sums of squares for testing hypothesis are available, called TYPE I, II, III and IV in the majority of statistical softwares.

Type I et Type III are the "standard" types printed by usual software (R, SAS...). They are equivalent in complete and balanced plan.

For a non-crossed or unbalanced design :

- Anova TYPE I will be different according to the order of appearance of the levels

- Anova TYPE III, correspond to the variability of an effect knowing that all the other effects are already present in the model

Type II and Type IV are optional types, Type II for hierarchical ANOVA and Type IV for missing cells designs.

## Explaining factors

1. Fixed effect factor :  
   All the levels are representative of the variability of the factor, they are the levels of interest.

2. Random effect factor :  
   The levels of the factor are not representative of all the variability of the factor, they are a sub-part of a larger population of different factor levels.
3. Nested factor  
   Interpretation of a factor (subordinate) is conditionned by the value taken by another factor (the superior)

```r
Factors can either be fixed or random.
A factor is fixed when the levels under study are the only levels of interest.
A factor is random when the levels under study are a random sample from a larger population and the goal of the study is to make a statement regarding the larger population.
```

The random factor also opens the way to the variance-covariance structure, that enables a better understanding of the factor.

_Nota bene_:  
According to the objective of the experiment, the factor can be random or fixed.
Ex : An experiment designed to evaluate the best baker will be analysed defining the "baker" factor (each participant) as a fixed effect.

Meanwhile if the objective is to evaluate wether the effect of the baker (in general) is important for the quality of the bread, then the "baker" factor will be random, as it is impossible to evaluate all the different bakers.

When building a model with interactions between factors, it is important to delete the non-significative interaction effects according to their complexity order (AnB < AnBnC) and re-evaluate the model.
