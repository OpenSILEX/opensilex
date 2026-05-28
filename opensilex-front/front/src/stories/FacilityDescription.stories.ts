import FacilityDescription from '../components/facilities/views/FacilityDescription.vue';

export default {
  title: 'Components/FacilityDescription',
  component: FacilityDescription,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilityDescription },
  setup() { return { args }; },
  template: '<FacilityDescription v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
