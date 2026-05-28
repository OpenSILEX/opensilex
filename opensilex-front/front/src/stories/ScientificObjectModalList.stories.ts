import ScientificObjectModalList from '../components/scientificObjects/ScientificObjectModalList.vue';

export default {
  title: 'Components/ScientificObjectModalList',
  component: ScientificObjectModalList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ScientificObjectModalList },
  setup() { return { args }; },
  template: '<ScientificObjectModalList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
