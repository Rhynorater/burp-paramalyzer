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

package com.professionallyevil.bc;

import burp.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Caret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

/**
 * Main class for the Correlator burp extension.
 */
public class Paramalyzer implements IBurpExtender, ITab, WorkerStatusListener, ClipboardOwner, IContextMenuFactory {
    private JPanel mainPanel;
    private JButton beginAnalysisButton;
    private JTextField textFieldStatus;
    private JProgressBar progressBar;
    protected JTable parametersTable;
    private JButton clearButton;
    private JList<String> listValues;
    private JTextArea textAreaRequest;
    private JTextArea textAreaResponse;
    private JCheckBox ignoreEmptyCheckBox;
    private JTextArea analysisTextArea;
    private JButton highlightValueButton;
    private JTextArea ignore;
    private JCheckBox showDecodedValuesCheckBox;
    private JTable cookieTable;
    private JButton saveCookieStatsButton;
    protected JTabbedPane tabPane;
    private JTabbedPane sessionsTabbedPane;
    private JTextArea sessionsHelpTextPane;
    private JCheckBox showEncodedValues;
    private JCheckBox showFormatPrefix;
    private JCheckBox showDuplicates;
    private IBurpExtenderCallbacks callbacks;
    private CorrelatorEngine engine = null;
    private ParametersTableModel paramsTableModel = new ParametersTableModel();
    private CookieStatisticsTableModel cookieStatisticsTableModel = new CookieStatisticsTableModel();
    private ParamListModel paramListModel = new ParamListModel();
    private int lastSelectedRow = -1;
    private IHttpRequestResponse displayedRequest = null;
    private int deepTabCount = 0;

    private static final boolean DEBUG_STATUS = true;
    private static final String VERSION = "2.1.1";
    private static final String EXTENSION_NAME = "Paramalyzer";

    public Paramalyzer() {
        parametersTable.setModel(paramsTableModel);
        parametersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        beginAnalysisButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paramsTableModel.setShowDecodedValues(showDecodedValuesCheckBox.isSelected());
                paramsTableModel.clear();
                lastSelectedRow = -1;
                textFieldStatus.setText("Analyzing...");
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                engine = new CorrelatorEngine(callbacks, Paramalyzer.this, ignoreEmptyCheckBox.isSelected(), ignore.getText());
                engine.execute();
            }
        });

        parametersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateParamInstanceList();

            }
        });

        cookieTable.setModel(cookieStatisticsTableModel);
        cookieTable.setAutoCreateRowSorter(true);

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldStatus.setText("");
                progressBar.setValue(0);
                paramsTableModel.clear();
                lastSelectedRow = -1;
                paramListModel.clear();
                textAreaRequest.setText("");
                textAreaResponse.setText("");
                analysisTextArea.setText("");
                cookieStatisticsTableModel.clear();
            }
        });

        listValues.setModel(paramListModel);

        listValues.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                IHttpRequestResponse message = paramListModel.getMessageForIndex(listValues.getSelectedIndex());
                if (message != null) {
                    textAreaRequest.setText(callbacks.getHelpers().bytesToString(message.getRequest()));
                    displayedRequest = message;

                    ParamInstance pi = paramListModel.getParamInstance(listValues.getSelectedIndex());

                    if (message.getResponse() != null && message.getResponse().length > 0) {
                        textAreaResponse.setText(callbacks.getHelpers().bytesToString(message.getResponse()));
                    } else {
                        textAreaResponse.setText("");
                    }

                    analysisTextArea.setText(ParamAnalyzer.analyze(pi, callbacks));
                } else {
                    callbacks.printOutput("Message was null for: " + listValues.getSelectedIndex());
                    textAreaResponse.setText("");
                    textAreaRequest.setText("");
                    analysisTextArea.setText("");
                    displayedRequest = null;

                }
                analysisTextArea.setCaretPosition(0);
                textAreaRequest.setCaretPosition(0);
                textAreaResponse.setCaretPosition(0);

            }
        });
        textAreaRequest.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                popup(e);
            }

            private void popup(MouseEvent e) {
                if (e.isPopupTrigger() && displayedRequest != null) { //if the event shows the menu
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(new AbstractAction() {
                        @Override
                        public Object getValue(String key) {
                            if (Action.NAME.equals(key)) {
                                return "Send to Repeater";
                            } else {
                                return super.getValue(key);
                            }
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            IRequestInfo info = callbacks.getHelpers().analyzeRequest(displayedRequest);
                            URL url = info.getUrl();
                            callbacks.sendToRepeater(url.getHost(), url.getPort(), url.getProtocol().toLowerCase().endsWith("s"), displayedRequest.getRequest(), null);
                        }
                    });

                    menu.add(new HighlighterAction("red"));
                    menu.add(new HighlighterAction("orange"));

                    menu.show(textAreaRequest, e.getX(), e.getY());
                }
            }
        });
        listValues.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                popup(e);
            }

            private void popup(MouseEvent e) {
                if (e.isPopupTrigger() && paramListModel.getSize() > 0) { //if the event shows the menu
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(new AbstractAction() {
                        @Override
                        public Object getValue(String key) {
                            if (Action.NAME.equals(key)) {
                                return "Copy List To Clipboard";
                            } else {
                                return super.getValue(key);
                            }
                        }

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            StringBuilder buf = new StringBuilder();
                            for (int i = 0; i < paramListModel.getSize(); i++) {
                                buf.append(paramListModel.getElementAt(i));
                                buf.append("\n");
                            }
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            StringSelection contents = new StringSelection(buf.toString());
                            clipboard.setContents(contents, Paramalyzer.this);
                        }
                    });

                    menu.add(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                int selected = listValues.getSelectedIndex();
                                if (selected > -1) {
                                    ParamInstance pi = paramListModel.getParamInstance(selected);
                                    setProgress(0);
                                    setStatus("Starting deep analysis of " + pi.getDecodedValue() + "...");
                                    String label = pi.getName();
                                    if (label.length() > 20) {
                                        label = label.substring(0, 17) + "...";
                                    }
                                    callbacks.printOutput("Starting deep analysis...");
                                    DeepAnalysisTab tab = new DeepAnalysisTab(pi, Paramalyzer.this, callbacks);
                                    tabPane.add(label, tab.getMainPanel());
                                    tab.setTitle(label);
                                    tabPane.setSelectedIndex(tabPane.getTabCount() - 1);
                                    tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, pi.getName() + "=" + pi.getDecodedValue());
                                    callbacks.customizeUiComponent(tab.getMainPanel());
                                    tab.begin();
                                }
                            } catch (Throwable t) {
                                callbacks.printError(t.getMessage());
                            }
                        }

                        @Override
                        public Object getValue(String key) {
                            if (Action.NAME.equals(key)) {
                                return "Deep Analysis";
                            } else {
                                return super.getValue(key);
                            }
                        }
                    });


                    menu.show(listValues, e.getX(), e.getY());
                }
            }
        });


        showDuplicates.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lastSelectedRow = -1;
                updateParamInstanceList();
            }
        });
        highlightValueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParamInstance pi = paramListModel.getParamInstance(listValues.getSelectedIndex());
                if (pi != null) {
                    Caret caret = textAreaRequest.getCaret();
                    caret.setSelectionVisible(true);
                    caret.setDot(pi.getValueStart());
                    caret.moveDot(pi.getValueEnd());
                }
            }
        });
        showDecodedValuesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateParamInstanceList();
                int row = parametersTable.getSelectedRow();
                paramsTableModel.setShowDecodedValues(showDecodedValuesCheckBox.isSelected());
                paramsTableModel.fireTableDataChanged();
                parametersTable.setRowSelectionInterval(row, row);
            }
        });

        saveCookieStatsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int result = chooser.showSaveDialog(mainPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    try {
                        PrintWriter w = new PrintWriter(new FileWriter(f));
                        StringBuilder buf = new StringBuilder();
                        for (int col = 0; col < cookieStatisticsTableModel.getColumnCount(); col++) {
                            buf.append(cookieStatisticsTableModel.getColumnName(col));
                            buf.append(',');
                        }
                        buf.deleteCharAt(buf.length() - 1);
                        w.println(buf.toString());

                        for (int row = 0; row < cookieStatisticsTableModel.getRowCount(); row++) {
                            buf = new StringBuilder();
                            for (int col = 0; col < cookieStatisticsTableModel.getColumnCount(); col++) {
                                buf.append(cookieStatisticsTableModel.getValueAt(row, col));
                                buf.append(',');
                            }
                            buf.deleteCharAt(buf.length() - 1);
                            w.println(buf.toString());
                        }

                        w.flush();
                        w.close();
                    } catch (IOException e1) {
                        callbacks.printError("Unable to write to file: " + e1.getMessage());
                    }
                }

            }
        });
        showEncodedValues.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paramListModel.showDecoded = !showEncodedValues.isSelected();
                lastSelectedRow = -1;
                updateParamInstanceList();
            }
        });
        showFormatPrefix.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paramListModel.showFormat = showFormatPrefix.isSelected();
                lastSelectedRow = -1;
                updateParamInstanceList();
            }
        });
    }

    private void updateParamInstanceList() {
        int selectedRow = parametersTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedRow = parametersTable.convertRowIndexToModel(selectedRow);
        }
        if (selectedRow != lastSelectedRow) {
            lastSelectedRow = selectedRow;
            //DetailsWorker worker = new DetailsWorker(paramsTableModel.getParameter(selectedRow), Correlator.this);
            //worker.execute();
            CorrelatedParam selectedParam = paramsTableModel.getParameter(selectedRow);
            textAreaRequest.setText("");
            textAreaResponse.setText("");
            analysisTextArea.setText("");
            paramListModel.setValues(selectedParam, showDuplicates.isSelected(), !showEncodedValues.isSelected());
            listValues.clearSelection();
        }
    }

    @Override
    public void setStatus(String statusText) {
        if (DEBUG_STATUS) {
            callbacks.printOutput("STATUS: "+statusText);
        }
        textFieldStatus.setText(statusText);
    }

    @Override
    public void setProgress(int percentDone) {
        progressBar.setValue(percentDone);
    }


    @Override
    public void done(Object result) {
        setStatus("Analysis complete.");
        progressBar.setValue(100);
        paramsTableModel.addParameters(engine.getUrlParameters());
        paramsTableModel.addParameters(engine.getBodyParameters());
        paramsTableModel.addParameters(engine.getCookieParameters());
        paramsTableModel.addParameters(engine.getJSONParameters());
        paramsTableModel.addParameters(engine.getRestParameters());
        cookieStatisticsTableModel.setCookieStatistics(engine.getCookieStatistics(), callbacks);
    }


    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.addSuiteTab(this);
        callbacks.setExtensionName(EXTENSION_NAME);
        callbacks.printOutput("Started " + EXTENSION_NAME + " version " + VERSION);
        callbacks.registerContextMenuFactory(this);
        callbacks.customizeUiComponent(mainPanel);
        callbacks.customizeUiComponent(sessionsHelpTextPane);
    }

    @Override
    public String getTabCaption() {
        return EXTENSION_NAME;
    }

    @Override
    public Component getUiComponent() {
        return mainPanel;
    }

    @Override
    public java.util.List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages != null && messages.length > 0) {
            java.util.List<JMenuItem> list = new ArrayList<>();
            IHttpRequestResponse selectedMessage = messages[0];
            final byte[] requestBytes = selectedMessage.getRequest();
            final IHttpService service = selectedMessage.getHttpService();
            JMenuItem menuItem = new JMenuItem("Send to Paramalyzer");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        SessionAnalysisTab tab = new SessionAnalysisTab(Paramalyzer.this, callbacks, requestBytes, service);
                        sessionsTabbedPane.addTab("S:" + sessionsTabbedPane.getTabCount(), tab.getSessionTabPanel());
                        tab.initializeTab();
                        callbacks.customizeUiComponent(tab.getSessionTabPanel());

                        Component tabComponent = tab.getSessionTabPanel();
                        if (tabComponent != null) {
                            Container parent = tabComponent.getParent();
                            if (parent instanceof JTabbedPane) {
                                ((JTabbedPane) parent).setSelectedComponent(tabComponent);
                            }

                            tabPane.setSelectedIndex(2);

                            Container mainParent = mainPanel.getParent();
                            if (mainParent instanceof JTabbedPane) {
                                ((JTabbedPane) mainParent).setSelectedComponent(mainPanel);
                            }
                        }


                        // sessionsTabbedPane.setSelectedIndex(sessionsTabbedPane.getTabCount() - 1);
                    } catch (Exception e1) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e1.printStackTrace(pw);
                        callbacks.printError(sw.toString());
                    }
                }
            });
            list.add(menuItem);
            return list;
        }

        return null;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setMinimumSize(new Dimension(800, 485));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tabPane = new JTabbedPane();
        panel1.add(tabPane, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabPane.addTab("Parameters", panel2);
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setOrientation(0);
        panel2.add(splitPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setMinimumSize(new Dimension(21, 180));
        splitPane1.setLeftComponent(scrollPane1);
        parametersTable = new JTable();
        parametersTable.setAutoCreateRowSorter(true);
        parametersTable.setFillsViewportHeight(true);
        scrollPane1.setViewportView(parametersTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1, true, false));
        splitPane1.setRightComponent(panel3);
        panel3.setBorder(BorderFactory.createTitledBorder("Details"));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder("Values"));
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setMaximumSize(new Dimension(120, 32767));
        panel4.add(scrollPane2, BorderLayout.CENTER);
        listValues = new JList();
        listValues.setSelectionBackground(new Color(-869022));
        listValues.setSelectionMode(0);
        scrollPane2.setViewportView(listValues);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, BorderLayout.NORTH);
        showEncodedValues = new JCheckBox();
        showEncodedValues.setSelected(true);
        showEncodedValues.setText("Encoded");
        showEncodedValues.setToolTipText("Show the encoded version of the parameter value.");
        panel5.add(showEncodedValues, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showFormatPrefix = new JCheckBox();
        showFormatPrefix.setText("Format Prefix");
        showFormatPrefix.setToolTipText("Show the format of each parameter instance as a [prefix] in the list.");
        panel5.add(showFormatPrefix, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showDuplicates = new JCheckBox();
        showDuplicates.setSelected(false);
        showDuplicates.setText("Duplicates");
        showDuplicates.setToolTipText("Show duplicate values in this list.");
        panel5.add(showDuplicates, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel3.add(panel6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder("What is it?"));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel6.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        analysisTextArea = new JTextArea();
        analysisTextArea.setEditable(false);
        analysisTextArea.setMinimumSize(new Dimension(100, 16));
        scrollPane3.setViewportView(analysisTextArea);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel3.add(panel7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel7.setBorder(BorderFactory.createTitledBorder("Req. / Resp."));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        panel7.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 2, false));
        tabbedPane1.setBorder(BorderFactory.createTitledBorder("Message"));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Request", panel8);
        final JScrollPane scrollPane4 = new JScrollPane();
        panel8.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textAreaRequest = new JTextArea();
        scrollPane4.setViewportView(textAreaRequest);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Response", panel9);
        final JScrollPane scrollPane5 = new JScrollPane();
        panel9.add(scrollPane5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textAreaResponse = new JTextArea();
        scrollPane5.setViewportView(textAreaResponse);
        highlightValueButton = new JButton();
        highlightValueButton.setText("Highlight Value");
        panel7.add(highlightValueButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabPane.addTab("Cookies", panel10);
        final Spacer spacer1 = new Spacer();
        panel10.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel10.add(scrollPane6, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        cookieTable = new JTable();
        scrollPane6.setViewportView(cookieTable);
        saveCookieStatsButton = new JButton();
        saveCookieStatsButton.setText("Save...");
        saveCookieStatsButton.setToolTipText("Save these results to a CSV file.");
        panel10.add(saveCookieStatsButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel10.add(spacer2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabPane.addTab("Sessions", panel11);
        sessionsTabbedPane = new JTabbedPane();
        panel11.add(sessionsTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        sessionsTabbedPane.addTab("Help", panel12);
        final JScrollPane scrollPane7 = new JScrollPane();
        scrollPane7.setEnabled(false);
        panel12.add(scrollPane7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        sessionsHelpTextPane = new JTextArea();
        sessionsHelpTextPane.setEditable(false);
        sessionsHelpTextPane.setLineWrap(true);
        sessionsHelpTextPane.setText("This tool will help determine which parameters are involved in maintaining session state, which can be particularly helpful when applications have a large number of cookies.\n\nTo perform session token analysis in Paramalyzer, find a working authenticated request in proxy history or from repeater, right-click, and \"Send to Paramalyzer\".  This will create a new tab next to this help tab.\n\nOnce in that tab, use the \"Verify Baseline\" button to make sure your request is  still producing authenticated responses, then press the \"Analyze\" button.");
        scrollPane7.setViewportView(sessionsHelpTextPane);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel13, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        panel13.setBorder(BorderFactory.createTitledBorder("Status"));
        textFieldStatus = new JTextField();
        textFieldStatus.setEditable(false);
        panel13.add(textFieldStatus, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        progressBar = new JProgressBar();
        panel13.add(progressBar, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane8 = new JScrollPane();
        panel14.add(scrollPane8, new GridConstraints(1, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane8.setBorder(BorderFactory.createTitledBorder("Ignore These"));
        ignore = new JTextArea();
        ignore.setText("__VIEWSTATE\n__VIEWSTATEGENERATOR");
        ignore.setToolTipText("List parameters with large values you want to skip over.");
        scrollPane8.setViewportView(ignore);
        ignoreEmptyCheckBox = new JCheckBox();
        ignoreEmptyCheckBox.setSelected(true);
        ignoreEmptyCheckBox.setText("Ignore Empty Values");
        ignoreEmptyCheckBox.setToolTipText("Skip processing parameters without values.");
        panel14.add(ignoreEmptyCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearButton = new JButton();
        clearButton.setText("Clear");
        panel14.add(clearButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        beginAnalysisButton = new JButton();
        beginAnalysisButton.setText("Analyze");
        beginAnalysisButton.setToolTipText("Begin analysis of all requests in scope.");
        panel14.add(beginAnalysisButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showDecodedValuesCheckBox = new JCheckBox();
        showDecodedValuesCheckBox.setSelected(true);
        showDecodedValuesCheckBox.setText("Show Decoded Values");
        panel14.add(showDecodedValuesCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    class HighlighterAction extends AbstractAction {
        String color;

        HighlighterAction(String color) {
            this.color = color;
        }

        @Override
        public Object getValue(String key) {
            if (Action.NAME.equals(key)) {
                return "Proxy Highlight " + color;
            } else {
                return super.getValue(key);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            displayedRequest.setHighlight(color);
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }
}
