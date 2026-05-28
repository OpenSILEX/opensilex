import FormSelector from '../components/common/forms/FormSelector.vue';

export default {
  title: 'Components/FormSelector',
  component: FormSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FormSelector },
  setup() { return { args }; },
  template: '<FormSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
