package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.LanguageServerManager
import com.github.garetht.typstsupport.languageserver.TypstLanguageServerManager
import com.github.garetht.typstsupport.notifier.Notifier
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import kotlinx.coroutines.runBlocking
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.SwingConstants

class SettingsConfigurable(
  private val pathValidator: PathValidator = DefaultPathValidator(),
  private val executionValidator: ExecutionValidator = DefaultExecutionValidator(
    DefaultProcessExecutor(), pathValidator,
  ),
  private val settings: SettingsState = SettingsState.getInstance(),
  private val languageServerManager: LanguageServerManager = TypstLanguageServerManager(),
) :
  BoundSearchableConfigurable("Typst Support Settings", "") {

  // UI Elements
  internal lateinit var fileField: Cell<TextFieldWithBrowseButton>
  internal lateinit var automaticRadioButton: Cell<JBRadioButton>
  internal lateinit var customRadioButton: Cell<JBRadioButton>
  internal lateinit var testResultLabel: Cell<JLabel>
  internal lateinit var testBinaryButton: Cell<JButton>
  internal lateinit var formatterDropdown: Cell<ComboBox<TypstFormatter>>

  // Component-internal state
  internal var binaryExecutionValidated: Boolean = false

  override fun createPanel(): DialogPanel {
    return panel {
      group("Tinymist") {
        buttonsGroup("Binary filepath") {
          row {
            automaticRadioButton = radioButton(
              "Use automatically downloaded binary (${VersionRequirement.downloadVersion.toPathString()})",
              BinarySource.USE_AUTOMATIC_DOWNLOAD
            )
          }

          row {
            customRadioButton =
              radioButton(
                "Specify local binary location", BinarySource.USE_CUSTOM_BINARY
              )
                .onChanged {
                  if (it.isSelected) {
                    this@SettingsConfigurable.resetTestResultLabel()
                  }
                }
          }

          row {
            fileField =
              textFieldWithBrowseButton(
                fileChooserDescriptor =
                  FileChooserDescriptorFactory.singleFile()
                    .withTitle("Select Binary File")
                    .withFileFilter { virtualFile ->
                      val extension = virtualFile.extension
                      extension == null || extension.lowercase() == "exe"
                    },
              )
                .enabledIf(customRadioButton.selected)
                .bindText(settings.state::customBinaryPath)
                .resizableColumn()
                .align(AlignX.FILL)
                .validationOnInput {
                  pathValidator.validateBinaryFile(it.text).toValidationInfo(this)
                }
                .onChanged {
                  binaryExecutionValidated = false
                  resetTestResultLabel()
                }
          }

          row {
            testBinaryButton = button("Test Binary") {
              testBinaryExecution(fileField.component.text)
            }
              .enabledIf(customRadioButton.selected)


            testResultLabel = label("")
              .visibleIf(customRadioButton.selected)

            testResultLabel.component.horizontalAlignment = SwingConstants.LEFT
          }
        }
          .bind(settings.state::binarySource)
      }

      group("Formatter") {
        row("Typst Formatter") {
          formatterDropdown = comboBox(TypstFormatter.entries)
            .bindItem({ settings.state.formatter }, { settings.state.formatter = it ?: TypstFormatter.entries.first() })
        }
      }
    }
  }

  private fun resetTestResultLabel() {
    testResultLabel.component.text = ""
    testResultLabel.component.icon = null
  }


  fun testBinaryExecution(binaryPath: String) {
    // Clear previous result
    this.resetTestResultLabel()

    // Run validation in background thread to avoid blocking UI
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val result = executionValidator.validateBinaryExecution(binaryPath)
        binaryExecutionValidated = when (result) {
          is ExecutionValidation.Failed -> false
          is ExecutionValidation.Success -> true
        }

        // Update UI on EDT
        ApplicationManager.getApplication().invokeLater({
          updateTestResult(result)
        }, ModalityState.any())
      } catch (e: Exception) {
        ApplicationManager.getApplication().invokeLater({
          updateTestResult(ExecutionValidation.Failed("Error during validation: ${e.message}"))
        }, ModalityState.any())
      }
    }
  }

  private fun updateTestResult(result: ExecutionValidation) {
    when (result) {
      is ExecutionValidation.Success -> {
        testResultLabel.component.icon = AllIcons.General.InspectionsOK
        testResultLabel.component.text = "Success: ${result.version.toConsoleString()}"
        testResultLabel.component.foreground = JBColor.GREEN
      }

      is ExecutionValidation.Failed -> {
        testResultLabel.component.icon = AllIcons.General.Error
        testResultLabel.component.text = "Failed: ${result.message}"
        testResultLabel.component.foreground = JBColor.RED
      }
    }
  }


  override fun apply() {
    if (customRadioButton.selected()) {
      val exception = pathValidator.validateBinaryFile(fileField.component.text)
        .toConfigurationException()
      if (exception != null) {
        throw exception
      }

      if (!binaryExecutionValidated) {
        throw ConfigurationException("The binary should be tested with Test Binary before applying")
      }
    }

    super.apply()

    ApplicationManager.getApplication().invokeLater {
      val projectManager = ApplicationManager.getApplication().service<ProjectManager>()
      val openProjects = projectManager.openProjects
      openProjects.forEach { project ->
        ApplicationManager.getApplication().executeOnPooledThread {
          runBlocking {
            if (project.isDisposed) {
              return@runBlocking
            }
            languageServerManager.initialStart(project)
            Notifier.info("Restarting Tinymist server...")
          }
        }
      }
    }
  }
}
