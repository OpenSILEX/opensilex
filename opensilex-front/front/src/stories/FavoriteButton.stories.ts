import FavoriteButton from '../components/common/buttons/FavoriteButton.vue';

export default {
  title: 'Components/FavoriteButton',
  component: FavoriteButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { FavoriteButton },
  setup() { return { args }; },
  template: '<FavoriteButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
