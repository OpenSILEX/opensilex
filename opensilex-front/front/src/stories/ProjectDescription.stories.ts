import ProjectDescription from '../components/projects/details/ProjectDescription.vue';

export default {
  title: 'Components/ProjectDescription',
  component: ProjectDescription,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectDescription },
  setup() { return { args }; },
  template: '<ProjectDescription v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
