#!/bin/bash
#******************************************************************************
# OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
# Copyright © INRAE 2024
#
# Script to generate a new OpenSILEX module skeleton.
# Usage: ./create-module.sh
#******************************************************************************

set -e

# ------------------------------------------------------------------
# Color helpers
# ------------------------------------------------------------------
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # No Color

info()    { echo -e "${CYAN}ℹ ${NC}$1"; }
success() { echo -e "${GREEN}✔ ${NC}$1"; }
warn()    { echo -e "${YELLOW}⚠ ${NC}$1"; }
error()   { echo -e "${RED}✖ ${NC}$1"; }
header()  { echo -e "\n${BOLD}${CYAN}$1${NC}"; }

# ------------------------------------------------------------------
# Resolve project root (directory containing this script)
# ------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

header "═══════════════════════════════════════════════════"
header "       OpenSILEX Module Skeleton Generator        "
header "═══════════════════════════════════════════════════"
echo ""

# ------------------------------------------------------------------
# 1. Gather configuration
# ------------------------------------------------------------------

# Module name
read -rp "$(echo -e "${BOLD}Module name${NC} (e.g. opensilex-mymodule) [opensilex-]: ")" MODULE_NAME
if [ -z "$MODULE_NAME" ]; then
    error "Module name is required."
    exit 1
fi

# If user typed just a suffix without prefix, offer the default opensilex- prefix
if [[ "$MODULE_NAME" != *-* ]]; then
    DEFAULT_NAME="opensilex-${MODULE_NAME}"
    read -rp "$(echo -e "${BOLD}Use '${DEFAULT_NAME}' as module name?${NC} [Y/n]: ")" USE_DEFAULT
    USE_DEFAULT="${USE_DEFAULT:-y}"
    if [[ "$USE_DEFAULT" =~ ^[Yy]$ ]]; then
        MODULE_NAME="$DEFAULT_NAME"
    fi
fi

MODULE_DIR="${SCRIPT_DIR}/${MODULE_NAME}"

if [ -d "$MODULE_DIR" ]; then
    error "Directory '${MODULE_NAME}' already exists at ${SCRIPT_DIR}"
    exit 1
fi

# Java package
# Derive a default Java package from the module name:
#   opensilex-mymodule -> org.opensilex.mymodule
#   inrae-sixtine      -> fr.inrae.sixtine
DEFAULT_JAVA_PKG="org.opensilex.$(echo "$MODULE_NAME" | sed 's/^opensilex-//' | sed 's/-/./g')"
read -rp "$(echo -e "${BOLD}Java package${NC} [${DEFAULT_JAVA_PKG}]: ")" JAVA_PACKAGE
JAVA_PACKAGE="${JAVA_PACKAGE:-$DEFAULT_JAVA_PKG}"

# Module class name
# Derive a default class name: opensilex-my-module -> MyModuleModule
DEFAULT_CLASS_SUFFIX=$(echo "$MODULE_NAME" | sed 's/^opensilex-//' | sed 's/^[a-z]*-//' | sed -r 's/(^|-)(\w)/\U\2/g')
DEFAULT_CLASS_NAME="${DEFAULT_CLASS_SUFFIX}Module"
read -rp "$(echo -e "${BOLD}Module class name${NC} [${DEFAULT_CLASS_NAME}]: ")" CLASS_NAME
CLASS_NAME="${CLASS_NAME:-$DEFAULT_CLASS_NAME}"

# Module version
read -rp "$(echo -e "${BOLD}Module version${NC} [\${revision}]: ")" MODULE_VERSION
MODULE_VERSION="${MODULE_VERSION:-\${revision}}"

# GroupId
read -rp "$(echo -e "${BOLD}Maven groupId${NC} [org.opensilex]: ")" GROUP_ID
GROUP_ID="${GROUP_ID:-org.opensilex}"

# Include front-end?
read -rp "$(echo -e "${BOLD}Include front-end scaffolding?${NC} [Y/n]: ")" INCLUDE_FRONT
INCLUDE_FRONT="${INCLUDE_FRONT:-y}"

echo ""
header "── Configuration Summary ──────────────────────────"
info "Module name    : ${BOLD}${MODULE_NAME}${NC}"
info "Directory      : ${BOLD}${MODULE_DIR}${NC}"
info "GroupId        : ${BOLD}${GROUP_ID}${NC}"
info "Java package   : ${BOLD}${JAVA_PACKAGE}${NC}"
info "Module class   : ${BOLD}${CLASS_NAME}${NC}"
info "Version        : ${BOLD}${MODULE_VERSION}${NC}"
info "Front-end      : ${BOLD}$( [[ "$INCLUDE_FRONT" =~ ^[Yy]$ ]] && echo "Yes" || echo "No" )${NC}"
echo ""

read -rp "$(echo -e "${BOLD}Proceed?${NC} [Y/n]: ")" CONFIRM
CONFIRM="${CONFIRM:-y}"
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    warn "Aborted."
    exit 0
fi

# ------------------------------------------------------------------
# 2. Derived variables
# ------------------------------------------------------------------
JAVA_PKG_PATH=$(echo "$JAVA_PACKAGE" | tr '.' '/')
YEAR=$(date +%Y)

# ------------------------------------------------------------------
# 3. Create directory structure
# ------------------------------------------------------------------
header "Creating module structure..."

mkdir -p "${MODULE_DIR}/src/main/java/${JAVA_PKG_PATH}"
mkdir -p "${MODULE_DIR}/src/test/java/${JAVA_PKG_PATH}"
success "Created src/main/java/${JAVA_PKG_PATH}"
success "Created src/test/java/${JAVA_PKG_PATH}"

# ------------------------------------------------------------------
# 4. Generate .gitignore
# ------------------------------------------------------------------
cat > "${MODULE_DIR}/.gitignore" << 'GITIGNORE_EOF'

.*/
!/.gitignore
target/
**/node_modules/
**/nb-configuration.xml
**/site/
**/front/dist
**/front/src/lib
**/front/types
**/opensilex.dev.ts
GITIGNORE_EOF
success "Created .gitignore"

# ------------------------------------------------------------------
# 5. Generate CHANGELOG.md
# ------------------------------------------------------------------
cat > "${MODULE_DIR}/CHANGELOG.md" << CHANGELOG_EOF
# ${MODULE_NAME}

## 1.0.0

* Initial module creation
CHANGELOG_EOF
success "Created CHANGELOG.md"

# ------------------------------------------------------------------
# 6. Generate pom.xml
# ------------------------------------------------------------------
cat > "${MODULE_DIR}/pom.xml" << POM_EOF
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!--
******************************************************************************
 OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 Copyright © INRAE ${YEAR}

 ${MODULE_NAME} pom.xml
******************************************************************************
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <artifactId>${MODULE_NAME}</artifactId>
    <packaging>jar</packaging>
    <name>${MODULE_NAME}</name>
    <version>${MODULE_VERSION}</version>

    <properties>
        <revision>BUILD-SNAPSHOT</revision>
        <skipFrontTypesGeneration>true</skipFrontTypesGeneration>
    </properties>

    <parent>
        <groupId>${GROUP_ID}</groupId>
        <artifactId>opensilex-module</artifactId>
        <version>\${revision}</version>
        <relativePath>../opensilex-module/pom.xml</relativePath>
    </parent>

    <dependencies>
        <dependency>
            <groupId>${GROUP_ID}</groupId>
            <artifactId>opensilex-core</artifactId>
            <version>\${revision}</version>
        </dependency>
    </dependencies>
</project>
POM_EOF
success "Created pom.xml"

# ------------------------------------------------------------------
# 7. Generate Java Module class
# ------------------------------------------------------------------
cat > "${MODULE_DIR}/src/main/java/${JAVA_PKG_PATH}/${CLASS_NAME}.java" << JAVA_EOF
//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE ${YEAR}
//******************************************************************************
package ${JAVA_PACKAGE};

import org.opensilex.OpenSilexModule;
import org.opensilex.server.extensions.APIExtension;

/**
 * ${CLASS_NAME} - OpenSILEX extension module.
 */
public class ${CLASS_NAME} extends OpenSilexModule implements APIExtension {

}
JAVA_EOF
success "Created ${CLASS_NAME}.java"

# ------------------------------------------------------------------
# 8. Generate front-end scaffolding (if requested)
# ------------------------------------------------------------------
if [[ "$INCLUDE_FRONT" =~ ^[Yy]$ ]]; then
    header "Creating front-end scaffolding..."

    FRONT_DIR="${MODULE_DIR}/front"
    mkdir -p "${FRONT_DIR}/src/components"
    mkdir -p "${FRONT_DIR}/src/lang"
    mkdir -p "${FRONT_DIR}/src/lib"
    touch "${FRONT_DIR}/src/lib/.gitkeep"

    # -- babel.config.js --
    cat > "${FRONT_DIR}/babel.config.js" << 'BABEL_EOF'
module.exports = {
    presets: [
      '@vue/cli-plugin-babel/preset',
      '@babel/preset-env'
    ]
  }
BABEL_EOF
    success "Created front/babel.config.js"

    # -- vue.config.js --
    cat > "${FRONT_DIR}/vue.config.js" << 'VUECONFIG_EOF'
const TerserPlugin = require('terser-webpack-plugin');
const path = require('path');

module.exports = {
    configureWebpack: {
        externals: {
            'vue': 'Vue',
            'vue-router': 'vue-router',
            'vuex': 'vuex',
            'node-fetch': 'node-fetch',
            'vee-validate': 'vee-validate'
        },
        resolve: {
            alias: {
                'vue$': path.resolve('../../node_modules/vue/dist/vue.esm.js')
            }
        },
        performance: {
            hints: false
        },
        optimization: {
            minimize: (process.env.NODE_ENV === 'production'),
            minimizer: [new TerserPlugin()]
        }
    }
};
VUECONFIG_EOF
    success "Created front/vue.config.js"

    # -- package.json --
    cat > "${FRONT_DIR}/package.json" << PKGJSON_EOF
{
    "name": "${MODULE_NAME}",
    "version": "0.1.0",
    "private": true,
    "scripts": {
        "serve": "vue-cli-service build --target lib --formats umd-min src/index.ts --mode development --watch",
        "build": "vue-cli-service build --target lib --formats commonjs,umd-min src/index.ts --mode production",
        "lint": "vue-cli-service lint",
        "check:outdated": "yarn outdated || cd .",
        "check:security": "yarn audit || cd ."
    },
    "devDependencies": {
        "@vue/cli-plugin-babel": "4.5.15",
        "@vue/cli-plugin-eslint": "4.5.15",
        "@vue/cli-plugin-router": "4.5.15",
        "@vue/cli-plugin-typescript": "4.5.15",
        "@vue/cli-plugin-vuex": "4.5.15",
        "@vue/cli-service": "4.5.15",
        "@vue/eslint-config-typescript": "7.0.0"
    },
    "eslintConfig": {
        "root": true,
        "env": {
            "node": true
        },
        "extends": [
            "plugin:vue/essential",
            "eslint:recommended",
            "@vue/typescript"
        ],
        "rules": {
            "no-unused-vars": "off",
            "no-var": "off",
            "prefer-const": "off",
            "vue/custom-event-name-casing": "off",
            "vue/no-unused-vars": "off"
        },
        "parserOptions": {
            "parser": "@typescript-eslint/parser"
        }
    },
    "browserslist": [
        "> 1%",
        "last 2 versions"
    ],
    "dependencies": {}
}
PKGJSON_EOF
    success "Created front/package.json"

    # -- tsconfig.json --
    cat > "${FRONT_DIR}/tsconfig.json" << 'TSCONFIG_EOF'
{
    "compilerOptions": {
        "target": "esnext",
        "module": "esnext",
        "strict": true,
        "jsx": "preserve",
        "importHelpers": true,
        "moduleResolution": "node",
        "experimentalDecorators": true,
        "esModuleInterop": true,
        "allowSyntheticDefaultImports": true,
        "strictPropertyInitialization": false,
        "noImplicitAny": false,
        "strictNullChecks": false,
        "sourceMap": true,
        "baseUrl": ".",
        "typeRoots": ["node_modules"],
        "paths": {
            "@/*": [
                "src/*"
            ]
        },
        "lib": [
            "esnext",
            "dom",
            "dom.iterable",
            "scripthost"
        ]
    },
    "include": [
        "src/**/*.ts",
        "src/**/*.tsx",
        "src/**/*.vue",
        "tests/**/*.ts",
        "tests/**/*.tsx"
    ],
    "exclude": [
        "node_modules"
    ]
}
TSCONFIG_EOF
    success "Created front/tsconfig.json"

    # -- shims-vue.d.ts --
    cat > "${FRONT_DIR}/src/shims-vue.d.ts" << 'SHIMS_EOF'
declare module '*.vue' {
  import Vue from 'vue'
  export default Vue
}
SHIMS_EOF
    success "Created front/src/shims-vue.d.ts"

    # -- lang files --
    cat > "${FRONT_DIR}/src/lang/${MODULE_NAME}-en.json" << 'LANG_EN_EOF'
{
}
LANG_EN_EOF

    cat > "${FRONT_DIR}/src/lang/${MODULE_NAME}-fr.json" << 'LANG_FR_EOF'
{
}
LANG_FR_EOF
    success "Created front/src/lang/ i18n files"

    # -- index.ts --
    cat > "${FRONT_DIR}/src/index.ts" << INDEXTS_EOF
/// <reference path="../../../opensilex-security/front/types/opensilex-security.d.ts" />
/// <reference path="../../../opensilex-core/front/types/opensilex-core.d.ts" />

export default {
    install(Vue, options) {
    },
    components: {
    },
    lang: {
        "fr": require("./lang/${MODULE_NAME}-fr.json"),
        "en": require("./lang/${MODULE_NAME}-en.json"),
    }
};
INDEXTS_EOF
    success "Created front/src/index.ts"
fi

# ------------------------------------------------------------------
# 9. Print next-steps instructions
# ------------------------------------------------------------------
echo ""
header "═══════════════════════════════════════════════════"
header "  ✔  Module '${MODULE_NAME}' created successfully!"
header "═══════════════════════════════════════════════════"
echo ""
header "── Next Steps ─────────────────────────────────────"
echo ""
info "Add the following snippets to the root ${BOLD}pom.xml${NC}:"
echo ""
echo -e "${YELLOW}1) In the <modules> section:${NC}"
echo ""
echo -e "        ${GREEN}<!-- ${MODULE_NAME} module -->${NC}"
echo -e "        ${GREEN}<module>${MODULE_NAME}</module>${NC}"
echo ""
echo -e "${YELLOW}2) In the <dependencies> section:${NC}"
echo ""
echo -e "        ${GREEN}<!-- ${MODULE_NAME} dependency -->${NC}"
echo -e "        ${GREEN}<dependency>${NC}"
echo -e "        ${GREEN}    <groupId>${GROUP_ID}</groupId>${NC}"
echo -e "        ${GREEN}    <artifactId>${MODULE_NAME}</artifactId>${NC}"
echo -e "        ${GREEN}    <version>\${revision}</version>${NC}"
echo -e "        ${GREEN}</dependency>${NC}"
echo ""
info "Then build with: ${BOLD}mvn install -DskipTests${NC}"
echo ""
