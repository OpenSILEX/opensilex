import EntitiesView from '../components/variables/views/EntitiesView.vue';

export default {
  title: 'Components/EntitiesView',
  component: EntitiesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EntitiesView },
  setup() { return { args }; },
  template: '<EntitiesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
