package bynarig

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

/**
 * Opens the IDE Terminal tool window and runs the `agr` command in a fresh shell tab.
 *
 * `agr` is the CLI entry point for Google Antigravity. This action gives developers a
 * single-click / single-shortcut way to jump into Antigravity from inside any JetBrains
 * IDE without leaving the editor or alt-tabbing to an external terminal emulator.
 *
 * Implementation notes:
 *  - The Terminal tool window belongs to the bundled `org.jetbrains.plugins.terminal`
 *    plugin, declared as a `<depends>` in `plugin.xml`.
 *  - We always create a brand-new shell tab named "Antigravity" so we never step on a
 *    shell the user already has running (e.g. `npm run dev`, an SSH session, etc.).
 *  - The action is wired into the Tools menu and bound to `Ctrl+Alt+G` by default
 *    (see `plugin.xml`). Both bindings can be customised by the user via Keymap.
 */
class OpenAntigravityCliAction : AnAction() {

    /** Sink for diagnostic output – surfaces in `idea.log` and Help → Show Log. */
    private val log = thisLogger()

    private companion object {
        /** Command sent to the freshly opened shell. Centralised so it is trivial to tweak. */
        const val COMMAND = "agy"

        /** Tab label users will see in the Terminal tool window. */
        const val TAB_NAME = "Antigravity"

        /** Tool window id of the bundled terminal – stable across IDE versions. */
        const val TERMINAL_TOOL_WINDOW_ID = "Terminal"

        /** Notification group declared in plugin.xml; used for user-visible error toasts. */
        const val NOTIFICATION_GROUP_ID = "Antigravity CLI Opener"
    }

    /**
     * Run `update()` on a background thread – this is the modern (2022.3+) requirement
     * for any AnAction whose enabled/disabled state depends on more than trivial checks.
     * Returning BGT keeps the IDE EDT responsive while menus are being computed.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Enable the action only when a project is open – we need a project to host the
     * Terminal tool window. The Tools menu is always project-scoped, but this guards
     * Search Everywhere and Find Action invocations from no-project contexts as well.
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    /**
     * Entry point invoked by the platform when the user triggers the action.
     * Marshals to the EDT because every interaction with the Terminal widget /
     * tool window UI must happen on the Event Dispatch Thread.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Tool window manipulation and widget creation are UI operations – schedule
        // them on the EDT. `invokeLater` plays nicely with modal dialogs already open.
        ApplicationManager.getApplication().invokeLater {
            launchAgyInTerminal(project)
        }
    }

    /**
     * Reveal the Terminal tool window, spin up a new shell session in the project's
     * base directory, and feed the `agr` command into it.
     *
     * Any exception is logged + surfaced as a balloon notification rather than bubbled
     * out – an action throwing through the platform produces an ugly fatal error popup.
     */
    private fun launchAgyInTerminal(project: Project) {
        try {
            // Make the terminal tool window visible before we touch any widgets,
            // otherwise the new tab may render in a hidden component.
            val terminalToolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow(TERMINAL_TOOL_WINDOW_ID)
            terminalToolWindow?.activate(/* runnable = */ null, /* autoFocusContents = */ true)

            // Create a fresh shell widget. `deferSessionStartUntilUiShown = true` is
            // important: it waits until the Swing component is actually attached to a
            // parent before launching the underlying PTY, which prevents the brief
            // "shell exited" race on some Linux/macOS setups.
            val widget = TerminalToolWindowManager.getInstance(project).createShellWidget(
                /* workingDirectory = */ project.basePath,
                /* tabName = */ TAB_NAME,
                /* requestFocus = */ true,
                /* deferSessionStartUntilUiShown = */ true,
            )

            // `sendCommandToExecute` types the string into the shell and appends the
            // appropriate line-ending, so it works regardless of the user's shell
            // (bash, zsh, fish, PowerShell). This is the API exposed by the unified
            // `com.intellij.terminal.ui.TerminalWidget` interface in 2024.1+.
            widget.sendCommandToExecute(COMMAND)
        } catch (t: Throwable) {
            log.warn("Failed to launch '$COMMAND' in the IDE terminal", t)
            notifyUser(
                project,
                "Could not open Antigravity CLI: ${t.message ?: t.javaClass.simpleName}",
            )
        }
    }

    /**
     * Show a non-blocking balloon in the IDE so the user understands the action failed.
     * Falls back silently if the notification group is unavailable (e.g. headless tests).
     */
    private fun notifyUser(project: Project, message: String) {
        runCatching {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(message, NotificationType.ERROR)
                .notify(project)
        }
    }
}