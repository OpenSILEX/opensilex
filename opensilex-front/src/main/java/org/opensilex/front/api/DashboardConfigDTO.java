package org.opensilex.front.api;

public class DashboardConfigDTO {

    private boolean showMetrics;
    private GraphConfigDTO graph1;
    private GraphConfigDTO graph2;
    private GraphConfigDTO graph3;

    public boolean getShowMetrics() {
        return showMetrics;
    }

    public void setShowMetrics(boolean showMetrics) {
        this.showMetrics = showMetrics;
    }

    public GraphConfigDTO getGraph1() {
        return graph1;
    }

    public void setGraph1(GraphConfigDTO graph1) {
        this.graph1 = graph1;
    }

    public GraphConfigDTO getGraph2() {
        return graph2;
    }

    public void setGraph2(GraphConfigDTO graph2) {
        this.graph2 = graph2;
    }

    public GraphConfigDTO getGraph3() {
        return graph3;
    }

    public void setGraph3(GraphConfigDTO graph3) {
        this.graph3 = graph3;
    }
}
