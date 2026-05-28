import Favorites from '../components/home/dashboard/Favorites.vue';

export default {
  title: 'Components/Favorites',
  component: Favorites,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { Favorites },
  setup() { return { args }; },
  template: '<Favorites v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
