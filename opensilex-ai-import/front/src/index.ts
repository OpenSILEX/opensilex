import { ApiServiceBinder } from './lib';

import AiImportView from './components/AiImportView.vue';
import AiChatPanel from './components/AiChatPanel.vue';
import AiChatMessage from './components/AiChatMessage.vue';
import WorkbookStructurePanel from './components/WorkbookStructurePanel.vue';
import ResolutionReportPanel from './components/ResolutionReportPanel.vue';
import MappingPanel from './components/MappingPanel.vue';
import CreationProposalCard from './components/CreationProposalCard.vue';
import ImportRowsGrid from './components/ImportRowsGrid.vue';
import ObjectSheetsPanel from './components/ObjectSheetsPanel.vue';
import FileTemplateList from './components/FileTemplateList.vue';
import CreationDraftZone from './components/CreationDraftZone.vue';
import FactorDeclarationZone from './components/FactorDeclarationZone.vue';
import ImportStepsPanel from './components/ImportStepsPanel.vue';
import AdvancedImportPanel from './components/AdvancedImportPanel.vue';
import SessionFilesPanel from './components/SessionFilesPanel.vue';
import SheetDescriptionPanel from './components/SheetDescriptionPanel.vue';
import SheetPreviewTable from './components/SheetPreviewTable.vue';
import MermaidDiagram from './components/MermaidDiagram.vue';

import fr from './lang/ai-import-fr.json';
import en from './lang/ai-import-en.json';

/**
 * Component identifiers follow the `{module}-{Name}` convention the router relies on to know which
 * bundle to load, so the name must carry no dash of its own.
 */
const components: { [key: string]: any } = {
    'opensilex-ai-import-AiImportView': AiImportView,
    'opensilex-ai-import-AiChatPanel': AiChatPanel,
    'opensilex-ai-import-AiChatMessage': AiChatMessage,
    'opensilex-ai-import-WorkbookStructurePanel': WorkbookStructurePanel,
    'opensilex-ai-import-ResolutionReportPanel': ResolutionReportPanel,
    'opensilex-ai-import-MappingPanel': MappingPanel,
    'opensilex-ai-import-CreationProposalCard': CreationProposalCard,
    'opensilex-ai-import-ImportRowsGrid': ImportRowsGrid,
    'opensilex-ai-import-ObjectSheetsPanel': ObjectSheetsPanel,
    'opensilex-ai-import-FileTemplateList': FileTemplateList,
    'opensilex-ai-import-CreationDraftZone': CreationDraftZone,
    'opensilex-ai-import-FactorDeclarationZone': FactorDeclarationZone,
    'opensilex-ai-import-ImportStepsPanel': ImportStepsPanel,
    'opensilex-ai-import-AdvancedImportPanel': AdvancedImportPanel,
    'opensilex-ai-import-SessionFilesPanel': SessionFilesPanel,
    'opensilex-ai-import-SheetDescriptionPanel': SheetDescriptionPanel,
    'opensilex-ai-import-SheetPreviewTable': SheetPreviewTable,
    'opensilex-ai-import-MermaidDiagram': MermaidDiagram,
};

const messages: { [locale: string]: any } = { fr, en };

const plugin = {
    install(app: any, options: any) {
        const $opensilex = app.$opensilex ?? app.config.globalProperties.$opensilex;
        ApiServiceBinder.with($opensilex.getServiceContainer());

        // Registered here rather than through a `components` field: the host loads a module by
        // calling `app.use(plugin)` and nothing else, so registration is the plugin's own job.
        for (const id in components) {
            app.component(id, components[id]);
        }

        for (const locale in messages) {
            $opensilex.$i18n.mergeLocaleMessage(locale, messages[locale]);
        }
    }
};

// Default export only: see the `exports: 'default'` note in vite.config.ts.
export default plugin;
