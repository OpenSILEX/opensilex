import CharacteristicsView from '../components/variables/views/CharacteristicsView.vue';

export default {
  title: 'Components/CharacteristicsView',
  component: CharacteristicsView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CharacteristicsView },
  setup() { return { args }; },
  template: '<CharacteristicsView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
