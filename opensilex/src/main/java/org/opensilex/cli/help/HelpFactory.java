//******************************************************************************
//                           HelpFactory.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli.help;

import picocli.CommandLine.Help;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.IHelpFactory;
import picocli.CommandLine.Model.CommandSpec;

/**
 * <pre>
 * This class implements picocli.CommandLine.IHelpFactory to generate good
 * looking help messages for OpenSilex commands with custom styles and colors
 *
 * see: <a href="https://picocli.info/#_usage_help_with_styles_and_colors">Picocli
 * Documentation</a>
 * </pre>
 *
 * @author Vincent Migot
 */
public class HelpFactory implements IHelpFactory {

    /**
     * Ascii Art header when displaying help command
     */
    public static final String CLI_HEADER_HEADING = "\n"
            + "         .+.\n"
            + "        +++++`.\n"
            + "      ;+;  ++.++`.\n"
            + "     ++     '+,  ++`.\n"
            + "   '+.        ++  `+++       ___                    ___  ___  _     ___ __  __\n"
            + "  ++           +++`  ++     / _ \\  _ __  ___  _ _  / __||_ _|| |   | __|\\ \\/ /\n"
            + "`++   ,;++++++++++++++++   | (_) || '_ \\/ -_)| ' \\ \\__ \\ | | | |__ | _|  >  <\n"
            + "++++++:`        ++   .++    \\___/ | .__/\\___||_||_||___/|___||____||___|/_/\\_\\\n"
            + " .++    `'+++   ++  ++`           |_|\n"
            + "   ++.      '+++'+.++\n"
            + "     ++:+++.`   .++\n"
            + "      ``````````\n";

    /**
     * Prefix for "Usage" help section
     */
    public static final String CLI_SYNOPSYS_HEADING = "\n@|bold,underline Usage|@: ";

    /**
     * Prefix for "usage" help section
     */
    public static final String CLI_DESCRIPTION_HEADING = "\n@|bold,underline Description|@:\n";

    /**
     * Prefix for "Paramters" help section
     */
    public static final String CLI_PARAMETER_LIST_HEADING = "\n@|bold,underline Parameters|@:\n";

    /**
     * Prefix for "Options" help section
     */
    public static final String CLI_OPTION_LIST_HEADING = "\n@|bold,underline Options|@:\n";

    /**
     * Prefix for "sub commands" help section
     */
    public static final String CLI_COMMAND_LIST_HEADING = "\n@|bold,underline Commands|@:\n";

    /**
     * "footer" help section
     */
    public static final String CLI_FOOTER = "\nCopyright(c) INRA - UMR MISTEA - 2019";

    /**
     * Initialize help layout values and return the
     * {@code picocli.CommandLine.Help} command
     *
     * @param commandSpec the command to create usage help for
     * @param colorScheme the color scheme to use when rendering usage help
     * @return a {@code picocli.CommandLine.Help} instance
     */
    @Override
    public Help create(CommandSpec commandSpec, ColorScheme colorScheme) {
        commandSpec.usageMessage().headerHeading(CLI_HEADER_HEADING);
        commandSpec.usageMessage().synopsisHeading(CLI_SYNOPSYS_HEADING);
        commandSpec.usageMessage().descriptionHeading(CLI_DESCRIPTION_HEADING);
        commandSpec.usageMessage().parameterListHeading(CLI_PARAMETER_LIST_HEADING);
        commandSpec.usageMessage().optionListHeading(CLI_OPTION_LIST_HEADING);
        commandSpec.usageMessage().commandListHeading(CLI_COMMAND_LIST_HEADING);
        commandSpec.usageMessage().footer(CLI_FOOTER);
        return new Help(commandSpec, colorScheme);
    }

}
