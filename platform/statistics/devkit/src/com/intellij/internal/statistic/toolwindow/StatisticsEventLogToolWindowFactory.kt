// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.NonEmptyActionGroup
import com.intellij.internal.statistic.StatisticsDevKitUtil.DEFAULT_RECORDER
import com.intellij.internal.statistic.actions.RecordStateStatisticsEventLogAction
import com.intellij.internal.statistic.eventLog.getEventLogProviders
import com.intellij.internal.statistic.eventLog.validator.rules.impl.TestModeValidationRule
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.ContentFactory


/**
 * Creates a toolwindow with feature usage statistics event log
 */
private class StatisticsEventLogToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    createNewTab(project, toolWindow, DEFAULT_RECORDER)
    toolWindow.setToHideOnEmptyContent(true)
    if (toolWindow is ToolWindowEx) {
      val newSessionActionGroup = createNewSessionActionGroup(project)
      toolWindow.setTabActions(newSessionActionGroup)
    }

    project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
      override fun toolWindowShown(id: String, toolWindow: ToolWindow) {
        if (eventLogToolWindowsId == id && toolWindow.isVisible && toolWindow.contentManager.contentCount == 0) {
          // open a new session if all tabs were closed manually
          createNewTab(project, toolWindow, DEFAULT_RECORDER)
        }
      }
    })
  }

  override fun isApplicable(project: Project) = TestModeValidationRule.isTestModeEnabled()
}

private fun createNewSessionActionGroup(project: Project): NonEmptyActionGroup {
  val actionGroup = NonEmptyActionGroup()
  actionGroup.isPopup = true
  actionGroup.templatePresentation.icon = AllIcons.General.Add

  val actions = getEventLogProviders().map { logger ->
    val recorder = logger.recorderId
    CreateNewSessionAction(project, recorder)
  }
  actionGroup.addAll(actions)
  return actionGroup
}

private class CreateNewSessionAction(private val project: Project, private val recorderId: String) : AnAction(recorderId) {
  override fun actionPerformed(e: AnActionEvent) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(eventLogToolWindowsId) ?: return
    createNewTab(project, toolWindow, recorderId)
  }
}

private fun createNewTab(project: Project, toolWindow: ToolWindow, recorderId: String) {
  val eventLogToolWindow = StatisticsEventLogToolWindow(project, recorderId)
  val content = ContentFactory.SERVICE.getInstance().createContent(eventLogToolWindow.component, recorderId, true)
  content.preferredFocusableComponent = eventLogToolWindow.component
  toolWindow.contentManager.addContent(content)
  RecordStateStatisticsEventLogAction.checkLogRecordingEnabled(project, recorderId)
}