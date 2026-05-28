import DeviceTypes from '../components/ontology/typesView/DeviceTypes.vue';

export default {
  title: 'Components/DeviceTypes',
  component: DeviceTypes,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeviceTypes },
  setup() { return { args }; },
  template: '<DeviceTypes v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
