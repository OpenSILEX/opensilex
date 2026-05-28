import GeometryForm from '../components/common/forms/GeometryForm.vue';

export default {
  title: 'Components/GeometryForm',
  component: GeometryForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GeometryForm },
  setup() { return { args }; },
  template: '<GeometryForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
