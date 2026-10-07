# Vocabulary regarding factors

Author: Jean-Eudes Hollebecq - Opensilex

## Experimental design

__Foreword:__ It is a good practice to create the experiment with the help of a statistician as early as possible. If the variables, factors, and experimental designs are properly defined, then the subsequent analysis becomes much easier and stronger.

1. Crossed design :   
 All the factor levels are represented
 Ex: Factor1 with 2 levels and factor2 with 5 levels; 2x5 = 10 combinations.
2. Factorial design = idem
3. Non-Crossed design :   
 Not a crossed design: some combinations are not represented.
4. Balanced design :   
 Same number of individuals for each combination of levels.
5. Unbalanced design :   
 Uneven number of individuals for each level.


The tool used to answer questions formulated like "does the effect of treatment X significantly impact the production of grain ?” is the ANalysis Of VAriance (ANOVA). This is the most common tool to answer statistical questions.


## Consequences on ANOVA

Several types of sums of squares are available for testing hypotheses. TYPES I, II, III, and IV are usually available in most statistical softwares (R, SAS, ...).
TYPE I and TYPE III are the "standard" types printed by usual software (R, SAS...). They are equivalent when using a complete and balanced plan.

For a non-crossed or unbalanced design :
- TYPE I Anova will be different depending on the order of appearance of the levels
- TYPE III Anova corresponds to the variability explained by adding an effect while all the other effects are already present in the model

TYPE II and TYPE IV are optional types used for hierarchical ANOVA and with missing data. In case of missing data, it is usually preferable to use a dedicated imputation method.


## Explaining factors

1. Fixed effect factors :   
 All the levels are representative of the variability of the factor, they are the levels of interest.
2. Random effect factors :   
 The levels of the factor are not representative of all the variability of the factor, they are only a part of a larger population of different factor levels.
3. Nested factor :   
 Interpretation of a factor (subordinate) is conditioned by the value taken by another factor (the superior)   

Factors can either be fixed or random.
A factor is fixed when the levels under study are the only levels of interest.
A factor is random when the levels under study are a random sample from a larger population and the goal of the study is to make a statement regarding the larger population.
Random factors also allow the study of the variance-covariance structure, which enables a better understanding of the factor.

__Note:__  
Depending on the objective of the experiment, the same factor can either be random or fixed.
Ex: In an experiment designed to evaluate the best baker, the "baker" factor (each participant) will be considered as a fixed effect. Meanwhile, if the objective is to evaluate whether the effect of the baker (in general) is important for the quality of the bread, then the "baker" factor will be considered random, as it is impossible to evaluate every single baker.  

When building a model with interactions between factors, it is important to remove the non-significative interaction effects according to their complexity order (AnB < AnBnC) and re-evaluate the model.
