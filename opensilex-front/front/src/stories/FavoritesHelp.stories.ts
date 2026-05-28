import FavoritesHelp from '../components/home/dashboard/FavoritesHelp.vue';

export default {
  title: 'Components/FavoritesHelp',
  component: FavoritesHelp,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FavoritesHelp },
  setup() { return { args }; },
  template: '<FavoritesHelp v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
