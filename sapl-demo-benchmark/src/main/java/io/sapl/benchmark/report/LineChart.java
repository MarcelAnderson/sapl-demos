/*
 * Copyright (C) 2017-2024 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.benchmark.report;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static io.sapl.benchmark.report.Utilities.getMaxValue;

public class LineChart {
    private final JFreeChart        chart;

    public LineChart(String title, DefaultCategoryDataset dataset, String valueAxisLabel) {
        chart = ChartFactory.createLineChart(title, "iteration", valueAxisLabel, dataset,
                PlotOrientation.VERTICAL,true, true, false);

        // add marks to the data points in the graph
        var plot = chart.getCategoryPlot();
        var renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setDefaultShapesVisible(true);
        plot.setRenderer(renderer);

        // format axis
        var maxValue = getMaxValue(dataset);
        var yAxis    = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRange(false);
        yAxis.setUpperBound(maxValue * 1.05);
        yAxis.setLowerBound(0);

    }

    public void saveToPNGFile(File file) throws IOException {
        saveToPNGFile(file, 900, 600);
    }

    public void saveToPNGFile(File file, int width, int height) throws IOException {
        var fos = Files.newOutputStream(file.toPath());
        ChartUtils.writeScaledChartAsPNG(fos, chart, width, height, 3, 3);
        fos.close();
    }
}
