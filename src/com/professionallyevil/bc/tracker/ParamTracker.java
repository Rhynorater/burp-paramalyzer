/*
 * Copyright (c) 2020 Jason Gillam
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

package com.professionallyevil.bc.tracker;

import burp.IBurpExtenderCallbacks;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.professionallyevil.bc.CorrelatedParam;
import com.professionallyevil.bc.Paramalyzer;
import com.professionallyevil.bc.WorkerStatusListener;
import com.professionallyevil.bc.graph.DirectionalGraphPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ParamTracker implements WorkerStatusListener {
    private JPanel mainPanel;
    private DirectionalGraphPanel<TrackedParameter> directionalGraph;
    private JButton analyzeButton;
    private JTextField progressText;
    private JProgressBar progressBar;
    private Paramalyzer paramalyzer;
    private IBurpExtenderCallbacks callbacks;

    public ParamTracker(Paramalyzer p) {
        this.paramalyzer = p;
        $$$setupUI$$$();
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    directionalGraph.getModel().clear();
                    initializeTracking();
                    callbacks.printOutput("Tracked parameters: " + directionalGraph.getModel().getVertices().size());

                } catch (Error ex) {
                    callbacks.printError(ex.getMessage());
                }
//                directionalGraph.getModel().addEdge("user", "session");
//                directionalGraph.getModel().addEdge("password", "session");
//                directionalGraph.getModel().addEdge("session", "account");
            }
        });
    }

    @Override
    public void setStatus(String statusText) {
        progressText.setText(statusText);
    }

    @Override
    public void setProgress(int percentDone) {
        progressBar.setValue(percentDone);
    }

    @Override
    public void done(Object result) {
        for(TrackedParameter param: directionalGraph.getModel().getVertices()) {
            for(TrackedParameter origin: param.origins) {
                directionalGraph.getModel().addEdge(origin, param);
            }
        }
    }

    void initializeTracking() {
        java.util.List<CorrelatedParam> params = paramalyzer.getParamSecrets();
        directionalGraph.getModel().clear();  // TODO: optimize so we don't have to reanalyze parameters that have not changed.

        for (CorrelatedParam param : params) {
            directionalGraph.getModel().addVertex(new TrackedParameter(param));
        }

        ParamTrackerInitializer initializer = new ParamTrackerInitializer(callbacks, directionalGraph.getModel().getVertices(), this);
        initializer.execute();

        try {
            callbacks.printOutput("Looping on initializer done...");

            while (!initializer.isDone()) {
                callbacks.printOutput("initializer not done.  Waiting a few millis...");
                Thread.currentThread().wait(500);
            }
            callbacks.printOutput("initializer done.");

        } catch (InterruptedException e) {
            callbacks.printOutput("initializer was interrupted.");
            e.printStackTrace();
        }
        callbacks.printOutput("Done initializing.  Parameters tracked: " + directionalGraph.getModel().getVertices().size());
    }

    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        directionalGraph.setCallbacks(callbacks);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 1, false));
        panel1.setBorder(BorderFactory.createTitledBorder("Values"));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder("Parameters"));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(30);
        scrollPane1.setVerticalScrollBarPolicy(20);
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        directionalGraph.setMinimumSize(new Dimension(250, 10));
        directionalGraph.setPreferredSize(new Dimension(0, 20));
        directionalGraph.setRequestFocusEnabled(false);
        scrollPane1.setViewportView(directionalGraph);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        analyzeButton = new JButton();
        analyzeButton.setText("Analyze");
        panel3.add(analyzeButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(1, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, -1), null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder("Status"));
        progressText = new JTextField();
        progressText.setEditable(false);
        panel4.add(progressText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        progressBar = new JProgressBar();
        panel4.add(progressBar, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(240, -1), null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        directionalGraph = new DirectionalGraphPanel<>();
    }
}
