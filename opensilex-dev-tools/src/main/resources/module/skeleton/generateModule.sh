# module_name  => .e.g : inrae-sixtine
# Module_name  => .e.g : Sixtine
# short_module_name  => .e.g : sixtine
# organisation  => .e.g : inrae

# module_name="sinfonia-module"

# To make a proper CLI
# See https://sookocheff.com/post/bash/parsing-bash-script-arguments-with-shopts/
# while getopts ":ht" opt; do
#   case ${opt} in
#     h ) # process option h
#       ;;
#     t ) # process option t
#       ;;
#     \? ) echo "Usage: cmd [-h] [-t]"
#       ;;
#   esac
# done

# With two arguments
input=$1
organisation_name=$2

short_module_name=${input,,*}
module_name=${short_module_name}-module
Module_name=${short_module_name^*}
# echo "input"
# echo ${input}
# echo "short_module_name"
# echo ${short_module_name}
# echo "module_name"
# echo ${module_name}
# echo "Module_name"
# echo ${Module_name}
# short_module_name="sinfonia"
module_revision="INSTANCE-SNAPSHOT"

java_module_path="src/main/java/$organisation_name/$module_name"

config_dir_path="template_config"

# create dir
cp -R $config_dir_path/module_skeleton $module_name
mkdir -p $module_name/$java_module_path
mkdir -p $module_name/front/theme/$short_module_name/images
mkdir -p $module_name/front/theme/$short_module_name/fonts

# create files
echo "{}" > $module_name/front/src/lang/$short_module_name-fr.json
echo "{}" > $module_name/front/src/lang/$short_module_name-en.json


# create conf
echo "{\"module_name\":\"${module_name}\",\"Module_name\":\"$Module_name\",\"short_module_name\":\"$short_module_name\",\"organisation_name\":\"$organisation_name\"}" > $config_dir_path/config.json

#replace files
mustache $config_dir_path/config.json $config_dir_path/pom.mustache >  $module_name/pom.xml
mustache $config_dir_path/config.json $config_dir_path/package.mustache >  $module_name/front/package.json
mustache $config_dir_path/config.json $config_dir_path/theme_module.mustache > $module_name/front/theme/$short_module_name/$short_module_name.yml
mustache $config_dir_path/config.json $config_dir_path/index-ts.mustache > $module_name/front/src/index.ts
mustache $config_dir_path/config.json $config_dir_path/FooterComponent.mustache >  $module_name/front/src/components/layout/${Module_name}FooterComponent.vue
mustache $config_dir_path/config.json $config_dir_path/HeaderComponent.mustache >  $module_name/front/src/components/layout/${Module_name}HeaderComponent.vue
mustache $config_dir_path/config.json $config_dir_path/HomeComponent.mustache >  $module_name/front/src/components/layout/${Module_name}HomeComponent.vue
mustache $config_dir_path/config.json $config_dir_path/LoginComponent.mustache >  $module_name/front/src/components/layout/${Module_name}LoginComponent.vue
mustache $config_dir_path/config.json $config_dir_path/MenuComponent.mustache >  $module_name/front/src/components/layout/${Module_name}MenuComponent.vue
mustache $config_dir_path/config.json $config_dir_path/ModuleClass.mustache > $module_name/src/main/java/$organisation_name/$module_name/${Module_name}Module.java
