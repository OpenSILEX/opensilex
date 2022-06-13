//******************************************************************************
//                           HelpOption.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import picocli.CommandLine.Option;

/**
 * Helper class used to add easily help functionality to any command.
 * <pre>
 * To add automatic help add:
 * - For a method command, add the following code to the method parameters
 * <code>@Mixin HelpOption help</code>
 * - For a class command, add the following code to the class members
 * <code>
 * &#64;Mixin
 * private HelpOption help = new HelpOption();
 * </code>
 * </pre>
 *
 * @author Vincent Migot
 */
public class HelpOption {

    public static final String HELP_COMMAND = "--help";
    public static final String HELP_ALIAS_COMMAND = "-h";


    /**
     * Default help names and description.
     */
    @Option(names = {HELP_ALIAS_COMMAND, HELP_COMMAND}, usageHelp = true, description = "Display this help and exit")
    private boolean help;

    /**
     * Determine if help flag is on.
     *
     * @return true if help is requested and false otherwise
     */
    public boolean isHelp() {
        return help;
    }
}
