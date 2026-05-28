import ProjectsView from '../components/projects/ProjectsView.vue';

export default {
  title: 'Components/ProjectsView',
  component: ProjectsView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectsView },
  setup() { return { args }; },
  template: '<ProjectsView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
