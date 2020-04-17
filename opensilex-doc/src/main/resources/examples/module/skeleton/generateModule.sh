# module_name  => .e.g : inrae-sixtine
# Module_name  => .e.g : Sixtine
# short_module_name  => .e.g : sixtine
module_name="inrae-sixtine"
Module_name="Sixtine"
short_module_name="sixtine"
module_revision="INSTANCE-SNAPSHOT"

java_module_path="src/main/java/inrae/inrae-sixtine"

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
echo "{\"module_name\":\"${module_name}\",\"Module_name\":\"$Module_name\",\"short_module_name\":\"$short_module_name\"}" > $config_dir_path/config.json

#replace files
mustache $config_dir_path/config.json $config_dir_path/pom.mustache >  $module_name/pom.xml
mustache $config_dir_path/config.json $config_dir_path/package.mustache >  $module_name/front/package.json
mustache $config_dir_path/config.json $config_dir_path/theme_module.mustache > $module_name/front/theme/$short_module_name/$short_module_name.yml
mustache $config_dir_path/config.json $config_dir_path/index-ts.mustache > $module_name/front/src/index.ts
mustache $config_dir_path/config.json $config_dir_path/FooterComponent.mustache >  $module_name/front/src/components/layout/${Module_name}FooterComponent.mustache
mustache $config_dir_path/config.json $config_dir_path/FooterComponent.mustache >  $module_name/front/src/components/layout/${Module_name}HeaderComponent.mustache
mustache $config_dir_path/config.json $config_dir_path/FooterComponent.mustache >  $module_name/front/src/components/layout/${Module_name}HomeComponent.mustache
mustache $config_dir_path/config.json $config_dir_path/FooterComponent.mustache >  $module_name/front/src/components/layout/${Module_name}LoginComponent.mustache
mustache $config_dir_path/config.json $config_dir_path/FooterComponent.mustache >  $module_name/front/src/components/layout/${Module_name}MenuComponent.mustache