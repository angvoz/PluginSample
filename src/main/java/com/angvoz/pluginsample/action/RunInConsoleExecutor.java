package com.angvoz.pluginsample.action;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * TODO
 */
public class RunInConsoleExecutor extends Executor {
	public static final Icon ICON_RUN = IconLoader.getIcon("/icons/run_yellow.svg", RunInConsoleExecutor.class);
	public static final Icon ICON_RUN_DISABLED = IconLoader.getIcon("/icons/run_disabled.svg", RunInConsoleExecutor.class);

	public static final String TOOLWINDOW_ID = "myrun.tool.window.id";
	@NonNls
	public static final String EXECUTOR_ID = "MyRunExecutor";

	@Override
	@NotNull
	public String getStartActionText() {
		return "Command Start";
	}

	@Override
	public String getToolWindowId() {
		return TOOLWINDOW_ID;
	}

	@Override
	public Icon getToolWindowIcon() {
		return ICON_RUN;
	}

	@Override
	@NotNull
	public Icon getIcon() {
		return AllIcons.Actions.Execute;
	}

	@Override
	public Icon getDisabledIcon() {
		return ICON_RUN_DISABLED;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	@NotNull
	public String getActionName() {
		return "Command";
	}

	@Override
	@NotNull
	public String getId() {
		return EXECUTOR_ID;
	}

	@Override
	public String getContextActionId() {
		return getClass().getName() +".context.action.id";
	}

	@Override
	public String getHelpId() {
		return null;
	}

	public static Executor getRunExecutorInstance() {
		return ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
	}
}
