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

package com.professionallyevil.paramalyzer.secrets;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.professionallyevil.paramalyzer.CorrelatedParam;
import com.professionallyevil.paramalyzer.ParametersTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class SecretHunter implements IBurpExtender {
    private JPanel mainPanel;
    private JTable secretsTable;
    private JButton importSecrets;
    private JButton removeImportedButton;
    private JButton addSecretButton;
    private JButton editSelectedButton;
    private JButton removeSelectedButton;
    private final SecretsTableModel secretsTableModel = new SecretsTableModel();
    private IBurpExtenderCallbacks callbacks;

    public SecretHunter(ParametersTableModel parametersTableModel) {
        secretsTable.setModel(secretsTableModel);
        importSecrets.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                for (CorrelatedParam correlatedParam : parametersTableModel.getEntries()) {
                    if (correlatedParam.setSecret()) {
                        ParameterSecret secret = new ParameterSecret(correlatedParam);
                        secretsTableModel.add(secret);
                    }
                }
            }
        });
        removeImportedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                secretsTableModel.removeImported();

            }
        });
        addSecretButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                AddModifySecret dialog = new AddModifySecret(null, new AddModifySecretListener() {
                    @Override
                    public void dialogAccepted(String name, boolean isRegex, String matchString) {
                        CustomSecret secret = new CustomSecret(name, isRegex, matchString);
                        secretsTableModel.add(secret);
                    }
                });
                dialog.pack();
                dialog.setLocationRelativeTo(mainPanel);
                dialog.setVisible(true);

            }
        });
        editSelectedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int selectedRow = secretsTable.getSelectedRow();
                    Secret secret = secretsTableModel.getSecret(selectedRow);
                    if (secret instanceof CustomSecret) {
                        CustomSecret customSecret = (CustomSecret) secret;
                        AddModifySecret dialog = new AddModifySecret(customSecret, new AddModifySecretListener() {
                            @Override
                            public void dialogAccepted(String name, boolean isRegex, String matchString) {
                                customSecret.setName(name);
                                if (isRegex) {
                                    customSecret.setRegex(matchString);
                                } else {
                                    customSecret.setExactMatch(matchString);
                                }
                                secretsTableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                            }
                        });
                        dialog.pack();
                        dialog.setLocationRelativeTo(mainPanel);
                        dialog.setVisible(true);

                    }

            }
        });
        removeSelectedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int[] selectedRows = secretsTable.getSelectedRows();
                java.util.List<Secret> toBeRemoved = new ArrayList<>();
                for (int i = 0; i < selectedRows.length; i++) {
                    toBeRemoved.add(secretsTableModel.getSecret(selectedRows[i]));
                }
                secretsTableModel.removeSecrets(toBeRemoved);
            }
        });

        //  TODO: Investigate - why adding this logic in seems to cause glitchy table rendering.
//        secretsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
//            @Override
//            public void valueChanged(ListSelectionEvent listSelectionEvent) {
//                if (secretsTable.getSelectedColumnCount() > 0) {
//                    editSelectedButton.setEnabled(secretsTableModel.getSecret(secretsTable.getSelectedRow()) instanceof CustomSecret);
//                }
//            }
//        });
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public Component getMainPanel() {
        return mainPanel;
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
        mainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        mainPanel.add(spacer2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        secretsTable = new JTable();
        scrollPane1.setViewportView(secretsTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importSecrets = new JButton();
        importSecrets.setText("Import Secrets");
        panel1.add(importSecrets, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        removeImportedButton = new JButton();
        removeImportedButton.setText("Remove Imported");
        panel1.add(removeImportedButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addSecretButton = new JButton();
        addSecretButton.setText("Add Secret");
        panel1.add(addSecretButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editSelectedButton = new JButton();
        editSelectedButton.setText("Edit Selected");
        panel1.add(editSelectedButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeSelectedButton = new JButton();
        removeSelectedButton.setText("Remove Selected");
        panel1.add(removeSelectedButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


}
