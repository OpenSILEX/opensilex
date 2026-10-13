import type { Preview } from "@storybook/vue3-vite";

// Prérequis côté hôte de la librairie (cf. README)
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "../../../theme/opensilex/variables.css";
import "../../../theme/opensilex/main.css";

import { library } from "@fortawesome/fontawesome-svg-core";
import { fas } from "@fortawesome/free-solid-svg-icons";

library.add(fas);
