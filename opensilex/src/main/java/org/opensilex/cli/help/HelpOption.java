//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli.help;

import picocli.CommandLine.*;

/**
 * Helper class used to add easily help functionality to any command with simply adding
 * - For a method command, add the following code to the method parameters
 * `@Mixin HelpOption help`
 * - For a class command, add the following code to the class members
 * `@Mixin`
   `private HelpOption help = new HelpOption();`
 */
public class HelpOption {
    
    /**
     * Default help names and description
     */
     @Option(names = { "-h", "--help"}, usageHelp = true, description = "Display this help and exit")
     private boolean help;
}
