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