import UriForm from '../components/common/forms/UriForm.vue';

export default {
  title: 'Components/UriForm',
  component: UriForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { UriForm },
  setup() { return { args }; },
  template: '<UriForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
