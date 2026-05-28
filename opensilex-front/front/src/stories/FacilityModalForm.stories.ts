import FacilityModalForm from '../components/facilities/FacilityModalForm.vue';

export default {
  title: 'Components/FacilityModalForm',
  component: FacilityModalForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityModalForm },
  setup() { return { args }; },
  template: '<FacilityModalForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
