import FacilityForm from '../components/facilities/FacilityForm.vue';

export default {
  title: 'Components/FacilityForm',
  component: FacilityForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityForm },
  setup() { return { args }; },
  template: '<FacilityForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
