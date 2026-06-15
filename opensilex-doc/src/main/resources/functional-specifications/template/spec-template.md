This document is a template for writing specifications for a particular feature. Please copy-paste
it into your own file. Please edit the first line of the **document history** table. Please fill
the _WARNING_ annotation if the document is not exhaustive; you can remove it if the specification treats
all topics concerning the feature. Please name the document without uppercase nor spaces nor underscore, use
hyphens if for spaces (e.g. my-feature-specifications.md).

Each section of the template is optional. If a section is not relevant for the specification of the
feature, feel free to remove it. You can change the order of sections, and add new sections if you
need it. All texts in {curly braces} are meant to be replaced. Before making a commit of your changes,
please update the table of contents (you can do this automatically with your IDE).

Specifications documents are not intended to contain technical details. They should focus on the needs from a user 
perspective. The Business logic section is a very detailed description of the rules that must be applied to implement
the feature, but always from a user perspective. Technical details should be in technical documentation, not in specifications.

As this documentation will be published thanks to VitePress, please use inverted quotes for anything including `<` or `>`.
Common exemples are for `<T>` or `<http://example.org/my-uri>`. Otherwise the VitePress build will fail. Try applying this
advise for brackets `{` as it sometimes lead to build failure too. To ensure your documentation's vitepress compatibility follow this [simple test](../../technical-documentation/vitepress-doc-publication.md)

These first five paragraphs of this template are to be removed before publishing your specification.

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
  * [functional requirements](#functional-requirements)
  * [Business logic](#business-logic)
  * [Documentation](#documentation)
<!-- TOC -->

## Definitions

- **{Term}** : {definition}

## functional requirements

{Briefly describe the user needs}

- Use case #{number}: As a {user category}, I want to {action}.

## Business logic

{If some specific business rules are applicable in the solution, describe them extensively in this
section. Business logic also includes authorization rules.}

## Documentation

{List internal and external not technical documentations relevant to the feature. For example, configuration
instructions}.

- see the [technical document]({path})
- see the [user documentation]({path})