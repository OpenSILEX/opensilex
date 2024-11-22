# Configuring Agroportal search

To enable Agroportal search in your instance, you have to configure the Agroportal API path and API key in your 
configuration file :

```yaml
core:
    agroportal:
        basePath: "https://agroportal.lirmm.fr"
        baseAPIPath: "https://data.agroportal.lirmm.fr"
        externalAPIKey: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
```

You can also specify ontologies to query for each variable component. Note that this will be the default value, but 
the user will still be able to choose the ontologies they query on AgroPortal.

```yaml

front:
    agroportal:
        entity:
            - AGROVOC
            - PO
        trait:
            - PATO
        method:
            - TRANSFORMON
        unit:
            - OBOE
```