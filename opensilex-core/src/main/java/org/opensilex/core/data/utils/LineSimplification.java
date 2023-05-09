package org.opensilex.core.data.utils;

import org.opensilex.core.data.api.DataSimpleGetDTO;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class LineSimplification {

    public static class Point extends AbstractMap.SimpleEntry<Double, Double> {
        Point(double key, double value) {
            super(key, value);
        }

        @Override
        public String toString() {
            return String.format("(%f, %f)", getKey(), getValue());
        }
    }

    private static double perpendicularDistance(Point pt, Point lineStart, Point lineEnd) {
        double dx = lineEnd.getKey() - lineStart.getKey();
        double dy = lineEnd.getValue() - lineStart.getValue();

        // Normalize
        double mag = Math.hypot(dx, dy);
        if (mag > 0.0) {
            dx /= mag;
            dy /= mag;
        }
        double pvx = pt.getKey() - lineStart.getKey();
        double pvy = pt.getValue() - lineStart.getValue();

        // Get dot product (project pv onto normalized direction)
        double pvdot = dx * pvx + dy * pvy;

        // Scale line direction vector and subtract it from pv
        double ax = pvx - pvdot * dx;
        double ay = pvy - pvdot * dy;

        return Math.hypot(ax, ay);
    }

    public static void ramerDouglasPeucker(List<DataSimpleGetDTO> pointList, double epsilon, List<DataSimpleGetDTO> out) {
        if (pointList.size() < 2) throw new IllegalArgumentException("Not enough points to simplify");

        // Find the point with the maximum distance from line between the start and end
        double dmax = 0.0;
        int index = 0;
        int end = pointList.size() - 1;
        for (int i = 1; i < end; ++i) {
            Point currPoint = new Point(i, Double.valueOf(pointList.get(i).getValue().toString()));
            Point startPoint = new Point(0, Double.valueOf(pointList.get(0).getValue().toString()));
            Point endPoint = new Point(end, Double.valueOf(pointList.get(end).getValue().toString()));
            double d = perpendicularDistance(currPoint, startPoint, endPoint);
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        // If max distance is greater than epsilon, recursively simplify
        if (dmax > epsilon) {
            List<DataSimpleGetDTO> recResults1 = new ArrayList<>();
            List<DataSimpleGetDTO> recResults2 = new ArrayList<>();
            List<DataSimpleGetDTO> firstLine = pointList.subList(0, index + 1);
            List<DataSimpleGetDTO> lastLine = pointList.subList(index, pointList.size());
            ramerDouglasPeucker(firstLine, epsilon, recResults1);
            ramerDouglasPeucker(lastLine, epsilon, recResults2);

            // build the result list
            out.addAll(recResults1.subList(0, recResults1.size() - 1));
            out.addAll(recResults2);
            if (out.size() < 2) throw new RuntimeException("Problem assembling output");
        } else {
            // Just return start and end points
            out.clear();
            out.add(pointList.get(0));
            out.add(pointList.get(pointList.size() - 1));
        }
    }
}

