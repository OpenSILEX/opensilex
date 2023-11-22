This document is a template for writing specifications for a particular feature. Please copy-paste
it into your own file. Please edit the first line of the **document history** table. Please fill
the _WARNING_ annotation if the document is not exhaustive; you can remove it if the specification treats
all topics concerning the feature.

Each section of the template is optional. If a section is not relevant for the specification of the
feature, feel free to remove it. You can change the order of sections, and add new sections if you
need it. All texts in {curly braces} are meant to be replaced. Before making a commit of your changes,
please update the table of contents (you can do this automatically with your IDE).

These first three paragraphs of this template are to be removed before publishing your specification.

# Specifications : [{category}] {title}

**Document history (please add a line when you edit the document)**

| Date | Editor(s) | OpenSILEX version | Comment           |
|------|-----------|-------------------|-------------------|
|      |           |                   | Document creation |

> ⚠️ _WARNING_ : This document is incomplete ! You can help by expanding it. ⚠️
>
> Currently covered topics :
>
> - {topic 1}
>
> Missing topics :
>
> - {topic 2}

## Table of contents

<!-- TOC -->
* [Specifications : [{category}] {title}](#specifications--category-title)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
    * [Business logic](#business-logic)
  * [Technical specifications](#technical-specifications)
    * [Technical definitions](#technical-definitions)
    * [Detailed explanations](#detailed-explanations)
    * [Tests](#tests)
    * [Environment](#environment)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **{Term}** : {definition}

## Needs

{Briefly describe the user needs}

- Use case #{number}: As a {user category}, I want to {action}.

### Non-functional requirements

{Describe all non-functional requirements in this section, and give precise metrics if possible.
Below are some common examples.}

- **{Performance}** : {limit of the acceptable performances (latency, execution time, etc.) for the feature}
- **{Security}** : {access restriction (which user should have access to the feature, are there potential
  vulnerabilities, etc.)}
- **{Ergonomy}** : {how accessible the feature should be (visual or textual representations, number of
  clicks needed to perform an action)}
- **{Reliability}** : {how resilient the feature should be (what happens if the user enters an invalid input,
  or if a service is unavailable)}

## Solution

{Describe the solution we chose in OpenSILEX. You can explain why this solution was chosen, which
other solutions were considered and why they were not kept.}

### Business logic

{If some specific business rules are applicable in the solution, describe them extensively in this
section. Business logic also includes authorization rules.}

## Technical specifications

### Technical definitions

- **{Term}** : {definition}

### Detailed explanations

{Describe the files, classes, methods, algorithms and architectural choices that are essential to the
solution. You can divide this section in subsections to keep it organized. For example, you can have
an API and Front-end subsections, but that is not mandatory.}

### Tests

{Describe the automatic tests related to this feature, where they are located and what they are supposed
to check.}

### Environment

{Describe the packages and libraries required for the solution, and the specific version if needed.}

## Limitations and improvements

{Describe the known limits of the solution. If you have potential solutions to suggest, you
can specify them here.}

## Documentation

{List internal and external documentations relevant to the feature. For example, configuration
instructions or an external library documentation website}.