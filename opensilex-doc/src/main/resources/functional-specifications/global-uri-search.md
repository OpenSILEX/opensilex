
# Specifications : Global URI search

**Document history (please add a line when you edit the document)**

| Date     | Editor(s)            | OpenSILEX version | Comment           |
|----------|----------------------|-------------------|-------------------|
| 26/08/24 | Maximilian Hart      | 1.3.2             | Document creation |

>
> Currently covered topics :
>
> - Context, what exactly does global uri search mean here?
> - Design Specifications (Front)
> - Note about back-end process
>


## Table of contents

<!-- TOC -->
* [Specifications : Global URI searh](#specifications--global-uri-searh)
  * [Table of contents](#table-of-contents)
  * [Use cases](#use-cases)
  * [Design specifications](#design-specifications)
    * [Search Box](#search-box)
    * [Result](#result)
  * [Brief note on back-end specification](#brief-note-on-back-end-specification)
<!-- TOC -->


## Use cases

The User wants to be able to **quickly** understand if a URI corresponds to an Opensilex element on the running instance.
And if-so, what is the type, label (if this type of object has a label), metadata attributes (publisher, published, updated)
and a link to the corresponding page or details pop-up.

Scientific objects can have a same URI across multiple experiments so they will have to be handled differently (see use case 4).

- Use case #1: The user can enter a URI in a search box in the application header and hit a search button. (Enter a URI)
- Use case #2: After hitting search the user either receives a URI non-existing message where the result should be, or a box containing the results. (Does the URI exist?)
- Use case #3: Inside the result box the default returned elements will be type, label, metadata and a link to details page or popup.
- Use case #4: For elements where multiple URIs can exist (for now only Scientific Objects), we will always show the information from the GLOBAL context. The global ScientificObject details page already contains links to the Experiments.


## Design specifications

### Search Box

As stated in the use-cases, a new search box will be added to the interface. It shall appear in the header to the left of the button panel on the right-hand side
(language, help, logout...). **This search box must contain a "URI" in it as a placeholder** so that the user doesn't assume a search for any keyword is possible.

### Result

It has been decided that a pop-up box is preferable, as the goal is to return minimal information.
Each returned data (Type, label...) will be placed on its own line.


## Brief note on back-end specification

A new service will have to be created, taking only a URI as parameter and returning a list of DTOs containing the data we need
(most of the time the list will be of size 1). The search order will be: 
- SPARQL then NOSQL.