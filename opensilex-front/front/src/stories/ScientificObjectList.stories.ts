import ScientificObjectList from '../components/scientificObjects/ScientificObjectList.vue';

export default {
  title: 'Components/ScientificObjectList',
  component: ScientificObjectList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ScientificObjectList },
  setup() { return { args }; },
  template: '<ScientificObjectList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
