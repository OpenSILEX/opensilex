Configuration How-to
====================

Ce document à pour but de lister des examples pratiques pour ajouter, modifier ou supprimer des paramètres de configuration dans OpenSilex.

Le fichier de configuration d'OpenSilex est un fichier [YAML](https://yaml.org/) où chaque section correspond à un module.

Ce fichier est ensuite automatiquement déserialisé vers un objet Java repésentant une interface.

Pour plus d'information sur les détails sur la configuration et les détails d'implémentation voir [ici](../architecture/main.md#Configuration)

# Comment connaitre les paramètres de configuration existants ?

Dans un terminal taper la commande suivante:

`opensilex system full-config`

# Comment ajouter une section de configuration pour un module ?

Dans la classe du module, implémenter les méthodes `getConfigId` et `getConfigClass` qui représentent respectivement l'identifiant de la section de configuration et l'interface de configuration associée.

```java
public class MyModule extends OpenSilexModule {

    @Override
    public Class<?> getConfigClass() {
        return MyModuleConfig.class;
    }

    @Override
    public String getConfigId() {
        return "my-module";
    }
}
```

Ensuite créer l'interface de configuration `MyModuleConfig`.

Les méthodes de l'interface DOIVENT être annotées avec `@ConfigDescription` pour être prises en compte.

Cette annotation fourni un description et éventuellement une valeur par défaut correspondant au type de retour de la méthode.

Bonne pratique: placer cette interface dans le même package que le module `MyModule`

Ci-dessous l'exemple de la configuration du `ServerModule`.

```java
public interface ServerConfig {

    @ConfigDescription(
            value = "Server public URI",
            defaultString = "http://localhost:8666/"
    )
    public String publicURI();

    @ConfigDescription(
            value = "Available application language list",
            defaultList = {OpenSilex.DEFAULT_LANGUAGE, "fr"}
    )
    public List<String> availableLanguages();

    @ConfigDescription(
            value = "Tomcat system properties"
    )
    public Map<String, String> tomcatSystemProperties();

    @ConfigDescription(
            value = "Enable Tomcat anti-thread lock mechanism with StuckThreadDetectionValve",
            defaultBoolean = true
    )
    public boolean enableAntiThreadLock();
}
```

Qui correspondrait au YAML suivant dans la configuration globale:

```java
my-module:
    publicURI: http://example.com
    availableLanguages: 
        - en
        -fr
    tomcatSystemProperties:
        p1: v1
        p2: v2
    enableAntiThreadLock: true
```

# Comment accéder à un paramètre de configuration ?

Si le code où vous souhaitez utiliser ce paramètres est:
- Un module
- Un service
- Une commande

Ou que vous avez accès à des instances de ces objets, vous pouvez alors accèder à l'instance d'OpenSilex à l'aide de la méthode `getOpenSilex`.

De là vous pouvez charger directement l'instance de configuration en connaissant sa classe et celle de son module:

```java
OpenSilex opensilex = obj.getOpenSilex();
ServerConfig config = opensilex.getModuleConfig(MyModule.class, MyConfig.class);
config.myParam(); // --> return parameter myParam value
```

# Quels osnt les types de données possibles comme paramètres

## Valeurs primitives

