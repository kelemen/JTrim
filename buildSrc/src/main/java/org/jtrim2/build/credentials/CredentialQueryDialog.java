package org.jtrim2.build.credentials;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

@SuppressWarnings("serial")
public final class CredentialQueryDialog extends JDialog {
    private static final Map<CredentialType, UserVariableFactory> FACTORY_MAP = variableFactoryMap();

    private final List<UserVariable> variablesToQuery;

    private CredentialQueryDialog(Collection<DisplayedCredentialDef> variablesToQuery) {
        super((Frame)null, true);

        this.variablesToQuery = toUserVariables(variablesToQuery);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private static Map<CredentialType, UserVariableFactory> variableFactoryMap() {
        Map<CredentialType, UserVariableFactory> result = new EnumMap<>(CredentialType.class);

        result.put(CredentialType.TYPE_BOOL, BoolVariable::new);
        result.put(CredentialType.TYPE_STRING, PasswordVariable::new);
        result.put(CredentialType.TYPE_INFO, InfoVariable::new);

        return Collections.unmodifiableMap(result);
    }

    private static List<UserVariable> toUserVariables(Collection<DisplayedCredentialDef> variables) {
        List<UserVariable> result = new ArrayList<>(variables.size());
        for (DisplayedCredentialDef variable: variables) {
            CredentialType typeName = variable.getCredentialType();

            UserVariableFactory variableFactory = FACTORY_MAP.get(typeName);
            if (variableFactory == null) {
                variableFactory = PasswordVariable::new;
            }

            result.add(variableFactory.createVariable(variable));
        }

        return result;
    }

    private ParallelGroup createParallelGroup(GroupLayout layout) {
        ParallelGroup group = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        for (UserVariable variable: variablesToQuery) {
            variable.addToParallel(group);
        }
        return group;
    }

    private SequentialGroup createSequentialGroup(GroupLayout layout) {
        SequentialGroup group = layout.createSequentialGroup();
        group.addContainerGap();

        boolean first = true;
        for (UserVariable variable: variablesToQuery) {
            if (!first) {
                group.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
            }
            first = false;

            variable.addToSequential(group);
        }
        group.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        return group;
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel();

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(createParallelGroup(layout))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(createSequentialGroup(layout))
        );
        return panel;
    }

    public static Map<Object, String> queryVariables(
            Collection<DisplayedCredentialDef> variablesToQuery) {

        CredentialQueryDialog dlg = new CredentialQueryDialog(variablesToQuery);
        return dlg.queryVariables();
    }

    private Map<Object, String> queryVariables() {
        if (variablesToQuery.isEmpty()) {
            return Collections.emptyMap();
        }

        getContentPane().removeAll();

        JPanel queryPanel = createQueryPanel();
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getRootPane().setDefaultButton(okButton);

        final AtomicBoolean okPressed = new AtomicBoolean(false);
        okButton.addActionListener(e -> {
            okPressed.set(true);
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(queryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))
        );

        setTitle("Enter credentials");
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        if (!okPressed.get()) {
            return null;
        }

        Map<Object, String> result = new HashMap<>();
        for (UserVariable variable: variablesToQuery) {
            result.put(variable.getDisplayedVariable().getId(), variable.getValue());
        }
        return result;
    }

    private static final class BoolVariable implements UserVariable {
        private final DisplayedCredentialDef variable;
        private final JCheckBox checkBox;

        public BoolVariable(DisplayedCredentialDef variable) {
            this.variable = Objects.requireNonNull(variable, "variable");
            this.checkBox = new JCheckBox(variable.getDisplayName());
        }

        @Override
        public DisplayedCredentialDef getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(checkBox);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(checkBox);
        }

        @Override
        public String getValue() {
            return Boolean.toString(checkBox.isSelected());
        }
    }

    private static final class InfoVariable implements UserVariable {
        private final DisplayedCredentialDef variable;
        private final JLabel label;

        public InfoVariable(DisplayedCredentialDef variable) {
            this.variable = Objects.requireNonNull(variable, "variable");
            this.label = new JLabel(variable.getDisplayName());
        }

        @Override
        public DisplayedCredentialDef getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(label);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(label);
        }

        @Override
        public String getValue() {
            return "";
        }
    }

    private static final class PasswordVariable implements UserVariable {
        private final DisplayedCredentialDef variable;
        private final JLabel label;
        private final JPasswordField value;

        public PasswordVariable(DisplayedCredentialDef variable) {
            this.variable = Objects.requireNonNull(variable, "variable");
            this.label = new JLabel(variable.getDisplayName());
            this.value = new JPasswordField("");
        }


        @Override
        public DisplayedCredentialDef getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(label);
            group.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
            group.addComponent(value, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(label);
            group.addComponent(value);
        }

        @Override
        public String getValue() {
            return new String(value.getPassword()).trim();
        }
    }

    private interface UserVariable {
        public DisplayedCredentialDef getDisplayedVariable();
        public void addToSequential(SequentialGroup group);
        public void addToParallel(ParallelGroup group);
        public String getValue();
    }

    private interface UserVariableFactory {
        public UserVariable createVariable(DisplayedCredentialDef variable);
    }
}
