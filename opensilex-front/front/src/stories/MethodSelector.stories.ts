import MethodSelector from '../components/variables/form/MethodSelector.vue';

export default {
  title: 'Components/MethodSelector',
  component: MethodSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { MethodSelector },
  setup() { return { args }; },
  template: '<MethodSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
