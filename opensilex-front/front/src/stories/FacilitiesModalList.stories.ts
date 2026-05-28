import FacilitiesModalList from '../components/facilities/FacilitiesModalList.vue';

export default {
  title: 'Components/FacilitiesModalList',
  component: FacilitiesModalList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FacilitiesModalList },
  setup() { return { args }; },
  template: '<FacilitiesModalList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
