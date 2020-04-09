CLI How-to
==========

Ce document à pour but de lister des examples pratiques autour de l'interface en ligne de commande d'OpenSilex.

Une commande se décompose en un groupe de commande `<MyGroupId>`, un identifiant de commande `<MyCommandId>` et des paramètres `<Params1>`, `<Params2>`, ...

Saisi en ligne de commande cela donne: `java -jar opensilex.jar [--DEBUG] [--CONFIG_FILE=...] <MyGroupId> <MyCommandId> <params...>`

Pour plus d'information sur les détails de l'intégration des commandes voir [ici](../architecture/main.md#CLI)

# Comment ajouter un groupe de commandes ?

Créer une classe pour représenter le groupe de commande.

Bonne pratique: placer cette commande dans un sous package `org.my.module.cli`

Cette classe DOIT étendre `CLIHelpPrinterCommand` et implémenter `OpenSilexCommand` pour fonctionner correctement.

Cette classe DOIT être annotée avec [`@Command`](https://picocli.info/#command-methods), pour plus de détail voir la documentation de Picocli

```java
@Command(
        name = "<my-group-id>",
        header = "Subcommand to group OpenSILEX <group> operations"
)
public class <MyGroupId>Commands extends CLIHelpPrinterCommand implements OpenSilexCommand {

}
```

# Comment ajouter une commande à un groupe ?

Créer une méthode dans la classe du groupe (voir point précédent) pour représenter le point d'entrée de la commande avec les paramètres.

Cette méthode DOIT être annotée avec [`@Command`](https://picocli.info/#command-methods).

Les paramètres DOIVENT être annotée avec [`@Option` ou `@Parameters`](https://picocli.info/#option-parameters-methods).

```java
@Command(
        name = "<my-group-id>",
        header = "Subcommand to group OpenSILEX <group> operations"
)
public class <MyGroupId>Commands {

    @Command(
            name = "my-command-id",
            header = "Execute <command>"
    )
    public void <MyCommandId>(
        @Option(names = {"-E", "--show-ends"}) boolean <Params1>,
        @Parameters(paramLabel = "FILE") File[] <Params2>
        ...
    ) {

        // COMMAND BODY FOR <MyCommandId>

    }

}
```

# Comment tester une commande ?

Dans une classe existante ou en en créant une au besoin, ajouter une fonction `main` comme dans l'example suivant.

Ensuite il suffit d'éxécuter cette méthode en ajustant les paramètres dans le code.

```java
@Command(
        name = "<my-group-id>",
        header = "Subcommand to group OpenSILEX <group> operations"
)
public class <MyGroupId>Commands {

    public static void main(String[] args) throws Exception {
        MainCommand.main(new String[]{
            "my-group-id",
            "my-command-id",
            "-E",
            "path/to/file/1",
            "path/to/file/2",
            ...
        });
    }

    @Command(
            name = "my-command-id",
    )
    public void <MyCommandId>(
        @Option(names = {"-E", "--show-ends"}) boolean <Params1>,
        @Parameters(paramLabel = "FILE") File[] <Params2>
        ...
    ) {
        // COMMAND BODY FOR <MyCommandId>
    }
}
```

# Comment configurer un alias pour la ligne de commande ?

Une fois le projet compilé avec Maven, le dossier `opensilex-release/target/opensilex` est accessible.

Ce dossier contient les fichiers `opensilex.sh` et `opensilex.cmd` pour lancer l'application sous Linux ou Windows.

Selon votre système d'exploitation, il suffit de configurer un alias de `opensilex` vers le fichier correspondant.