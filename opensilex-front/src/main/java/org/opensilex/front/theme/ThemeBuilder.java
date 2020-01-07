/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.theme;

import io.bit3.jsass.CompilationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.opensilex.OpenSilexModule;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.OutputStyle;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

public class ThemeBuilder {

    private final OpenSilexModule module;

    private final String themeId;

    private final ThemeConfig config;

    public ThemeBuilder(OpenSilexModule module, String themeId, ThemeConfig config) {
        this.module = module;
        this.themeId = themeId;
        this.config = config;
    }

    private String buildStyleSheets(List<String> stylesheets, String basePath) throws IOException, URISyntaxException, CompilationException {

        String globalCss = "";

        for (String stylesheet : stylesheets) {
            String stylesheetFile = basePath + stylesheet;
            if (module.fileExists(stylesheetFile)) {
                if (stylesheetFile.endsWith(".scss")) {
                    globalCss += buildSass(stylesheetFile) + "\n";
                } else {
                    globalCss += IOUtils.toString(module.getFileInputStream(stylesheetFile), StandardCharsets.UTF_8.name()) + "\n";
                }

            }
        }

        return globalCss;
    }

    private String buildSass(String scssFile) throws IOException, URISyntaxException, CompilationException {
        String scss = IOUtils.toString(module.getFileInputStream(scssFile), StandardCharsets.UTF_8.name());
        final Compiler compiler = new Compiler();
        Options options = new Options();
        options.setOutputStyle(OutputStyle.EXPANDED);
        Output output = compiler.compileString(scss, options);
        return output.getCss();
    }

    public String buildCss() throws ThemeException {
        try {
            String basePath = "front/theme/" + themeId + "/";
            return buildStyleSheets(config.stylesheets(), basePath);
        } catch (IOException ex) {
            throw new ThemeException("IO Error while building theme: " + themeId, ex);
        } catch (URISyntaxException ex) {
            throw new ThemeException("Invalid theme path: " + themeId, ex);
        } catch (CompilationException ex) {
            throw new ThemeException("Error while compiling theme scss files: " + themeId, ex);
        }
    }
}
