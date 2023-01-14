package org.jtrim2.build.credentials

import java.awt.FlowLayout
import java.awt.Frame
import java.util.Collections
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.GroupLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.LayoutStyle

data class DisplayedCredentialDef(val id: Any, val displayName: String, val credentialType: CredentialType)

private val factoryMap: Map<CredentialType, ((DisplayedCredentialDef) -> UserVariable)> = run {
    val result: MutableMap<CredentialType, ((DisplayedCredentialDef) -> UserVariable)> = EnumMap(CredentialType::class.java)
    result[CredentialType.TYPE_BOOL] = { BoolVariable(it) }
    result[CredentialType.TYPE_STRING] = { PasswordVariable(it) }
    result[CredentialType.TYPE_INFO] = { InfoVariable(it) }
    Collections.unmodifiableMap(result)
}

class CredentialQueryDialog private constructor(variablesToQuery: Collection<DisplayedCredentialDef>) : JDialog(null as Frame?, true) {
    private val variablesToQuery = toUserVariables(variablesToQuery)

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }

    private fun createParallelGroup(layout: GroupLayout): GroupLayout.ParallelGroup {
        val group = layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        for (variable in variablesToQuery) {
            variable.addToParallel(group)
        }
        return group
    }

    private fun createSequentialGroup(layout: GroupLayout): GroupLayout.SequentialGroup {
        val group = layout.createSequentialGroup()
        group.addContainerGap()
        var first = true
        for (variable in variablesToQuery) {
            if (!first) {
                group.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
            }
            first = false
            variable.addToSequential(group)
        }
        group.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        return group
    }

    private fun createQueryPanel(): JPanel {
        val panel = JPanel()

        val layout = GroupLayout(panel)
        panel.layout = layout
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(createParallelGroup(layout))
                                .addContainerGap())
        )
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(createSequentialGroup(layout))
        )
        return panel
    }

    private fun queryVariables(): Map<Any, String>? {
        if (variablesToQuery.isEmpty()) {
            return emptyMap()
        }

        contentPane.removeAll()

        val queryPanel = createQueryPanel()
        val buttonPanel = JPanel(FlowLayout())
        val okButton = JButton("Ok")
        val cancelButton = JButton("Cancel")

        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)

        getRootPane().defaultButton = okButton

        val okPressed = AtomicBoolean(false)
        okButton.addActionListener {
            okPressed.set(true)
            dispose()
        }
        cancelButton.addActionListener { dispose() }

        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(queryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                        .addComponent(buttonPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE.toInt())
        )
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))
        )

        title = "Enter credentials"
        pack()
        setLocationRelativeTo(null)
        isVisible = true

        if (!okPressed.get()) {
            return null
        }

        val result: MutableMap<Any, String> = HashMap()
        for (variable in variablesToQuery) {
            result[variable.displayedVariable.id] = variable.getValue()
        }
        return result
    }

    companion object {
        private fun toUserVariables(variables: Collection<DisplayedCredentialDef>): List<UserVariable> {
            val result = ArrayList<UserVariable>(variables.size)
            for (variable in variables) {
                val typeName: CredentialType = variable.credentialType

                var variableFactory = factoryMap[typeName]
                if (variableFactory == null) {
                    variableFactory = ::PasswordVariable
                }

                result.add(variableFactory(variable))
            }
            return result
        }

        fun queryVariables(variablesToQuery: Collection<DisplayedCredentialDef>): Map<Any, String>? {
            val dlg = CredentialQueryDialog(variablesToQuery)
            return dlg.queryVariables()
        }
    }
}

private class BoolVariable(variable: DisplayedCredentialDef) : UserVariable {
    override val displayedVariable: DisplayedCredentialDef = variable
    private val checkBox = JCheckBox(variable.displayName)

    override fun addToSequential(group: GroupLayout.SequentialGroup) {
        group.addComponent(checkBox)
    }

    override fun addToParallel(group: GroupLayout.ParallelGroup) {
        group.addComponent(checkBox)
    }

    override fun getValue(): String = checkBox.isSelected.toString()
}

private class InfoVariable(variable: DisplayedCredentialDef) : UserVariable {
    override val displayedVariable: DisplayedCredentialDef = variable
    private val label = JLabel(variable.displayName)

    override fun addToSequential(group: GroupLayout.SequentialGroup) {
        group.addComponent(label)
    }

    override fun addToParallel(group: GroupLayout.ParallelGroup) {
        group.addComponent(label)
    }

    override fun getValue(): String = ""
}

private class PasswordVariable(variable: DisplayedCredentialDef) : UserVariable {
    override val displayedVariable: DisplayedCredentialDef = variable
    private val label: JLabel = JLabel(variable.displayName)
    private val passwordField = JPasswordField("")

    override fun addToSequential(group: GroupLayout.SequentialGroup) {
        group.addComponent(label)
        group.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        group.addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
    }

    override fun addToParallel(group: GroupLayout.ParallelGroup) {
        group.addComponent(label)
        group.addComponent(passwordField)
    }

    override fun getValue(): String = String(passwordField.password).trim()
}

private interface UserVariable {
    val displayedVariable: DisplayedCredentialDef

    fun addToSequential(group: GroupLayout.SequentialGroup)
    fun addToParallel(group: GroupLayout.ParallelGroup)
    fun getValue(): String
}
