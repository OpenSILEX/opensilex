import ExportDataFileModal from '../components/data/ExportDataFileModal.vue';

export default {
  title: 'Components/ExportDataFileModal',
  component: ExportDataFileModal,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExportDataFileModal },
  setup() { return { args }; },
  template: '<ExportDataFileModal v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
