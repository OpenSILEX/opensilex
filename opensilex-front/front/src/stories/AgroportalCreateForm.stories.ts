import AgroportalCreateForm from '../components/common/external-references/agroportal/wizard/AgroportalCreateForm.vue';

export default {
  title: 'Components/AgroportalCreateForm',
  component: AgroportalCreateForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalCreateForm },
  setup() { return { args }; },
  template: '<AgroportalCreateForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
