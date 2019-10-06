OpenSILEX - Coding style
================================================================================

# Use Netbeans automatic formater
--------------------------------------------------------------------------------

Automatic formater is accessible in menu Source -> Format

Default shortcut is `Alt+Shif-F`

You can define a custom one in menu Tools -> Options -> Keymap.

Existing shortcut profiles exists if your are used to another IDE like Eclipse or Emacs

# Naming convention
--------------------------------------------------------------------------------

## Java

Use Java standart naming convention for packages, classes, constants and attributes.

Package: `org.opensilex`

Class: `org.opensilex.OpenSilex`

Constant: `public final static String DEV_PROFILE_ID = "dev";`

Attribute: `private ModuleManager moduleManager;`

For acronyms used in Camel Case (Upper or lower), use capital letters for small 
less than 6 charaters or consider them as words.

Short exemple: DTO --> objectDTO, SPARQL --> SPARQLService

Long exemple: // TODO

## YAML

Use Lower Camel Case format for YAML configuration keys.

Example: 
```
opensilex:
    sparql:
        rdf4j:
            serverURI: http://localhost:8080/rdf4j-server/
            repository: opensilex
    
    bigData:
        mongodb:
            database: opensilex
```

## RDF

Use UpperCamelCase for concept classes names.

Use lowerCamelCase for relations names.

## Path and URI

Use preferably kebab-case for Path and URI.

All words are lower case and separated by "-".
