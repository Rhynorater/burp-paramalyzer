/*
 * Copyright (c) 2019 Jason Gillam
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

package com.professionallyevil.bc.graph;

import burp.IBurpExtenderCallbacks;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class DirectionalGraphPanel<T> extends JPanel implements MouseListener, GraphModelListener {

    DirectionalGraphModel<T> model = new DirectionalGraphModel<>();
    VertexRenderer<T> renderer = new DefaultVertexRenderer<>();
    T focus = null;
    IBurpExtenderCallbacks callbacks;  // TODO: temporary

    public DirectionalGraphPanel(){
        this.addMouseListener(this);
    }

    public void setCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.printOutput("Callbacks set for DirectionalGraphPanel");
    }

    public DirectionalGraphPanel(VertexRenderer<T> renderer) {
        this.renderer = renderer;
    }

    private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;

        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};

        g.drawLine(x1, y1, x2, y2);
        g.fillPolygon(xpoints, ypoints, 3);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        model.planLayout();
        // Calculate all the sizes
        int[] colWidths = new int[model.getColumns().size()];
        int maxHeight = 0;
        int totalWidth = 0;
        for(int col=0 ; col< model.getColumns().size(); col++) {
            int colHeight = 0;
            for(T vertex: model.getColumns().get(col)) {
                colWidths[col] = Math.max(colWidths[col], renderer.getWidth(vertex, g));
                colHeight = colHeight + renderer.getHeight(vertex, g) + model.rowGap;
            }
            maxHeight = Math.max(maxHeight, colHeight);
            totalWidth += colWidths[col];
            totalWidth += model.columnGap;
        }
        maxHeight = maxHeight + model.rowGap;

        Dimension newSize = new Dimension(totalWidth, maxHeight);
        setSize(newSize);
        setPreferredSize(newSize);

        // Render Vertices
        int xOffset = model.columnGap / 2;

        for(int col=0 ; col< model.getColumns().size(); col++) {
            int rowSpacing = maxHeight / (model.getColumns().get(col).size() + 1);

            int yOffset = rowSpacing;

            int xCenter = xOffset + colWidths[col] / 2;
            xOffset = xOffset + colWidths[col] + model.columnGap;

            for(T vertex: model.getColumns().get(col)) {
                if (vertex.equals(focus)) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.BLACK);
                }
                renderer.render(vertex, g, xCenter, yOffset);

                model.vertices.get(vertex).setDimensions(renderer.getWidth(vertex, g), renderer.getHeight(vertex, g));
                model.vertices.get(vertex).setPosition(xCenter, yOffset);
                yOffset += rowSpacing;
            }
        }

        // Render Directional edges
        for (T child: model.parentMap.keySet()) {
            for (T parent: model.parentMap.get(child)) {
                if(parent.equals(focus) || child.equals(focus)) {
                    g.setColor(Color.BLUE);
                }else {
                    g.setColor(Color.BLACK);
                }

                if(!child.equals(parent)) {
                    VertexInfo childInfo = model.vertices.get(child);
                    VertexInfo parentInfo = model.vertices.get(parent);
                    drawArrowLine(g, parentInfo.getXRight(), parentInfo.getYRight(), childInfo.getXLeft(), childInfo.getYLeft(), 12, 5);
                } else {
                    VertexInfo childInfo = model.vertices.get(child);
                    g.drawArc(childInfo.getXLeft(), childInfo.getYLeft() - renderer.getHeight(child, g), renderer.getWidth(child, g), 20,  0, 180);
                }
            }
        }
    }

    private static void createAndShowGUI() {
        System.out.println("Created GUI on EDT? "+
                SwingUtilities.isEventDispatchThread());
        JFrame f = new JFrame("Graph Layout Test");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation(dim.width/2-f.getSize().width/2, dim.height/2-f.getSize().height/2);
        DirectionalGraphPanel<String> panel = new DirectionalGraphPanel<>();
        panel.model.addEdge("User", "SessionID");
        panel.model.addEdge("Password", "SessionID");
        panel.model.addEdge("SessionID", "email");
        panel.model.addEdge("SessionID", "SSN");
        panel.model.addEdge("User", "SSN");
        panel.model.addEdge("SessionID", "SessionID");
        panel.model.printModel();
        f.add(panel);
        f.pack();
        f.setVisible(true);
    }

    public DirectionalGraphModel<T> getModel() {
        return model;
    }

    public void setRenderer(VertexRenderer<T> renderer){
        this.renderer = renderer;
    }

    private void setFocus(T vertex) {
        this.focus = vertex;
        repaint();
    }

    private void choseFocus(int x, int y) {
        boolean hit = false;
        for(T vertex: model.vertices.keySet()) {
            VertexInfo vi = model.vertices.get(vertex);
            if (x > vi.getXLeft() &&  x < vi.getXRight()) {  // inside X bounds
                if (y > vi.getYTop() && y < vi.getYBottom()) {
                    hit = true;
                    setFocus(vertex);
                    break;
                }
            }
        }
        if (!hit) {
            setFocus(null);
        }
    }

    @Override
    public void graphModelUpdated() {
        revalidate();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        choseFocus(e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}