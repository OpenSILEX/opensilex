import UnitsView from '../components/variables/views/UnitsView.vue';

export default {
  title: 'Components/UnitsView',
  component: UnitsView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { UnitsView },
  setup() { return { args }; },
  template: '<UnitsView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
