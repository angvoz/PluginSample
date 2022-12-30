package com.angvoz.pluginsample.action;

import com.angvoz.pluginsample.process.ProcessHandlerRIC;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * TODO
 */
public class RunInConsoleAction extends DumbAwareAction {
	private static final Logger LOG = Logger.getInstance(RunInConsoleAction.class);

	@Override
	public void actionPerformed(AnActionEvent e) {
		final Project project = e.getProject();
		if (project == null) return;

//		String commandStr = "C:\\Windows\\System32\\PING.exe  -n  10  localhost";
		String commandStr = "C:\\Windows\\System32\\cmd";
		String[] commandArr = commandStr.split("\\s+");

		runCommandInConsole(project, commandArr, commandStr);
	}

	private void runCommandInConsole(@NotNull final Project project, @NotNull final String[] command, String consoleTitle) {
		final Process process;

		ProcessBuilder builder = new ProcessBuilder(command);
		try {
			process = builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


		final ProcessHandler myProcessHandler = new ProcessHandlerRIC(process, consoleTitle) {
			@Override
			public boolean isSilentlyDestroyOnClose() {
				return true;
			}
		};

		final ConsoleManager consoleManager = new ConsoleManager(project, myProcessHandler);
		Disposer.register(project, consoleManager);
		consoleManager.withRerun(new Runnable() {
			@Override
			public void run() {
				myProcessHandler.destroyProcess();
				myProcessHandler.waitFor(2000L);
				runCommandInConsole(project, command, consoleTitle);
			}
		});
		consoleManager.withTitle(consoleTitle);
		consoleManager.withStop(new Runnable() {
			@Override
			public void run() {
				myProcessHandler.destroyProcess();
			}
		}, new Computable<Boolean>() {
			@Override
			public Boolean compute() {
				return !myProcessHandler.isProcessTerminated();
			}
		});
		consoleManager.runInConsole();
	}
}
