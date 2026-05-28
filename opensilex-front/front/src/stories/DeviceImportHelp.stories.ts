import DeviceImportHelp from '../components/devices/csv/DeviceImportHelp.vue';

export default {
  title: 'Components/DeviceImportHelp',
  component: DeviceImportHelp,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceImportHelp },
  setup() { return { args }; },
  template: '<DeviceImportHelp v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
