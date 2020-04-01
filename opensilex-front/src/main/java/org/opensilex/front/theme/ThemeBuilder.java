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

    private final ThemeBuilder parent;

    public ThemeBuilder(OpenSilexModule module, String themeId, ThemeConfig config, ThemeBuilder parent) {
        this.module = module;
        this.themeId = themeId;
        this.config = config;
        this.parent = parent;
    }

    private String buildStyleSheets(List<String> stylesheets, String basePath, List<String> excludes) throws IOException, URISyntaxException, CompilationException, ThemeException {

        String globalCss = "";

        if (this.parent != null) {
            globalCss = this.parent.buildCss(excludes);
        }

        for (String stylesheet : stylesheets) {
            if (!excludes.contains(stylesheet)) {
                String stylesheetFile = basePath + stylesheet;
                if (module.fileExists(stylesheetFile)) {
                    globalCss += IOUtils.toString(module.getFileInputStream(stylesheetFile), StandardCharsets.UTF_8.name()) + "\n";
                }
            }
        }

        return buildSass(globalCss);
    }

    private String buildSass(String scssContent) throws IOException, URISyntaxException, CompilationException {
        final Compiler compiler = new Compiler();
        Options options = new Options();
        options.setOutputStyle(OutputStyle.EXPANDED);
        Output output = compiler.compileString(scssContent, options);
        return output.getCss();
    }

    public String buildCss(List<String> excludes) throws ThemeException {
        try {
            String basePath = "front/theme/" + themeId + "/";
            return buildStyleSheets(config.stylesheets(), basePath, excludes);
        } catch (IOException ex) {
            throw new ThemeException("IO Error while building theme: " + themeId, ex);
        } catch (URISyntaxException ex) {
            throw new ThemeException("Invalid theme path: " + themeId, ex);
        } catch (CompilationException ex) {
            throw new ThemeException("Error while compiling theme scss files: " + themeId, ex);
        }
    }

    public String buildCss() throws ThemeException {
        return buildCss(this.config.excludes());
    }
}
