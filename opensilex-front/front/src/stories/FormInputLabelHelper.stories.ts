import FormInputLabelHelper from '../components/common/forms/FormInputLabelHelper.vue';

export default {
  title: 'Components/FormInputLabelHelper',
  component: FormInputLabelHelper,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FormInputLabelHelper },
  setup() { return { args }; },
  template: '<FormInputLabelHelper v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
