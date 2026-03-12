Developers :

Sebastien Prado (sebastien.prado@inrae.fr) <br>
Date : 2023-10-16 <br>
OpenSILEX version : 1.1.0 (develop)


# Sauvegarde de pages 

Modifications du système de pagination à plusieurs niveaux dans l'optique de conserver la dernière page selectionnée. Celui-ci est établis au niveau des listes globales (liste des objets scientifiques par exemple), et au niveau des onglets disposant de listes paginées. Les différents cas de figure pouvant être rencontrés durant la navigation sur le SI, et les fichiers associés à  sont détaillés ci-dessous.


- `TableAsyncView.vue` : 

1. Dans la fonction `created` est vérifié si l'on consulte ou non un onglet. Si oui la page passée à la pagination sous `currentPage`prend la valeur stockée dans le storage sous la clé `tabPage`, de la même façon la variable `currentTabPath` prend la valeur du nom de l'onglet stocké sous la clé `tabPath`. Si non, le changement reste le même qu'auparavant.


2. Un `watcher` réagit aux changements de `currentPage` et compare les informations tirées de l'URI visité à celles des clés présentes dans le storage. 

   2.1. Si le nom de la section est le même que celui stocké : <br>
     <br>&nbsp; &nbsp;    2.1.1 Et qu'un second paramètre existe (visite d'un onglet) et est le même que celui stocké : `currentPage`prend la valeur de la page de cet onglet stocké. <br>
     <br>&nbsp;&nbsp; 2.1.2 et qu'un second paramètre existe et est différent de celui stocké (autre onglet): on attribue à la clé `tabPath` le nom du nouvel onglet, à la clé `tabPage` la valeur "1", et à la  variable `curentPage` la valeur "1" également. <br>
     <br>&nbsp;&nbsp; 2.1.3 et qu'il n'existe pas de second paramètre (listes globales): on défini la valeur de `currentPage` comme étant celle de la clé `page`. <br><br>

   2.2. si le nom de section n'est pas le même que celui stocké (nouvelle section) : on défini la valeur de la clé `startPath` comme étant le nom de la section visitée, la valeur de sa page sous la clé `page` et celle de la page des onglets sous la clé `tabPage` toutes deux à "1".

 

3. À l'émission d'un événement de changement de page depuis `b-pagination` appel de la fonction `pageChange()` laquelle prendra la valeur de la page consultée et l'affectera à `currentPage` en plus de l'affecter à la clé `tabPage` ou `page` selon que l'on soit dans un onglet ou non.

- Composants des onglets de détail des différent modules comportant des listes paginées : <br>
À la visite de l'onglet de détail on initialise et récupère le nom de l'onglet visité dans `tabPath` puis on initialise la page visité à "1" dans`tabPage`. De cette façon à chaque changement de section, la visite de l'onglet "description" permet de redéfinir le nouvel onglet et les résultats à afficher comme étant ceux de la page 1.