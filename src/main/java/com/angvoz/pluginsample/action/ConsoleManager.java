package com.angvoz.pluginsample.action;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.*;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithActions;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 * Copy of com.intellij.execution.RunContentExecutor Runs a execution and prints the output in a content tab within the
 * Run toolwindow.
 *
 */
public class ConsoleManager implements Disposable {
	private final Project myProject;
	private final ProcessHandler myProcess;
	private final List<Filter> myFilterList = new ArrayList<>();
	private Runnable myRerunAction;
	private Runnable myStopAction;
	private Runnable myAfterCompletion;
	private Computable<Boolean> myStopEnabled;
	private String myTitle = "Output";
	private String myHelpId = null;
//	private boolean myActivateToolWindow = true;
	private boolean myActivateToolWindow = true;

	public ConsoleManager(@NotNull Project project, @NotNull ProcessHandler process) {
		myProject = project;
		myProcess = process;
	}

	public ConsoleManager withFilter(Filter filter) {
		myFilterList.add(filter);
		return this;
	}

	public ConsoleManager withTitle(String title) {
		myTitle = title;
		return this;
	}

	public ConsoleManager withRerun(Runnable rerun) {
		myRerunAction = rerun;
		return this;
	}

	public ConsoleManager withStop(@NotNull Runnable stop, @NotNull Computable<Boolean> stopEnabled) {
		myStopAction = stop;
		myStopEnabled = stopEnabled;
		return this;
	}

	public ConsoleView createConsole(@NotNull Project project, @NotNull ProcessHandler process, List<Filter> myFilterList) {
		TextConsoleBuilderImpl consoleBuilder = (TextConsoleBuilderImpl) TextConsoleBuilderFactory.getInstance().createBuilder(project);
		consoleBuilder.setUsePredefinedMessageFilter(false);
		consoleBuilder.filters(myFilterList);
		ConsoleView console = consoleBuilder.getConsole();

		List<Filter> filters = ConsoleViewUtil.computeConsoleFilters(project, console, GlobalSearchScope.allScope(project));
		for (Filter filter : filters) {
			console.addMessageFilter(filter);
		}

		console.print("==> " + process.toString() + "\n", ConsoleViewContentType.NORMAL_OUTPUT);

		console.attachToProcess(process);
		return console;
	}

	public void runInConsole() {
		FileDocumentManager.getInstance().saveAllDocuments();

		ConsoleView consoleView = createConsole(myProject, myProcess, myFilterList);

		if (myHelpId != null) {
			consoleView.setHelpId(myHelpId);
		}
		Executor executor = RunInConsoleExecutor.getRunExecutorInstance();
		DefaultActionGroup actions = new DefaultActionGroup();

		// Create runner UI layout
		final RunnerLayoutUi.Factory factory = RunnerLayoutUi.Factory.getInstance(myProject);
		final RunnerLayoutUi layoutUi = factory.create("run.in.console", "Run In Console", "RunInConsole", this);

		final JComponent consolePanel = createConsolePanel(consoleView, actions);
		RunContentDescriptor descriptor = new RunContentDescriptor(new RunProfile() {
			@Nullable
			@Override
			public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
				return null;
			}

			@Override
			public String getName() {
				return myTitle;
			}

			@Nullable
			@Override
			public Icon getIcon() {
				return RunInConsoleExecutor.ICON_RUN;
			}
		}, new DefaultExecutionResult(consoleView, myProcess), layoutUi);
		descriptor.setExecutionId(System.nanoTime());
		descriptor.setFocusComputable(() -> consoleView.getPreferredFocusableComponent());
		descriptor.setAutoFocusContent(true);
		descriptor.setContentToolWindowId(RunInConsoleExecutor.TOOLWINDOW_ID);


		ComponentWithActions componentWithActions = new MyImpl(null, null, (JComponent) consoleView, null, consolePanel);
		final Content content = layoutUi.createContent(ExecutionConsole.CONSOLE_CONTENT_ID, componentWithActions, myTitle, AllIcons.Debugger.Console, consolePanel);
//		content.setDescription("file.getAbsolutePath()");   //todo does nothing
		layoutUi.addContent(content, 0, PlaceInGrid.right, false);
//		layoutUi.getOptions().setLeftToolbar(createActionToolbar(consolePanel, consoleView, layoutUi, myProcess, descriptor, executor), "RunnerToolbar");

		Disposer.register(myProject, descriptor);
		Disposer.register(descriptor, this);
		Disposer.register(descriptor, content);

		Disposer.register(content, consoleView);
		if (myStopAction != null) {
			Disposer.register(consoleView, new Disposable() {
				@Override
				public void dispose() {
					myStopAction.run();
				}
			});
		}

		for (AnAction action : consoleView.createConsoleActions()) {
			actions.add(action);
		}

		ToolWindowManager.getInstance(myProject).invokeLater(new Runnable() {
			@Override
			public void run() {
				RunContentManager.getInstance(myProject).showRunContent(executor, descriptor);

				if (myActivateToolWindow) {
					activateToolWindow();
				}

				if (myAfterCompletion != null) {
					myProcess.addProcessListener(new ProcessAdapter() {
						@Override
						public void processTerminated(ProcessEvent event) {
							SwingUtilities.invokeLater(myAfterCompletion);
						}
					});
				}

				myProcess.startNotify();
			}
		});
	}

//	@NotNull
//	private ActionGroup createActionToolbar(JComponent consolePanel, ConsoleView consoleView, @NotNull final RunnerLayoutUi myUi,
//											ProcessHandler processHandler, RunContentDescriptor contentDescriptor, Executor runExecutorInstance) {
//		final DefaultActionGroup actionGroup = new DefaultActionGroup();
//		actionGroup.add(new RerunAction(consolePanel));
//		actionGroup.add(new StopAction());
//		actionGroup.add(new PinAction(processHandler));
//		actionGroup.add(myUi.getOptions().getLayoutActions());
//		CloseAction closeAction = new CloseAction(runExecutorInstance, contentDescriptor, myProject) {
//			@Override
//			public void actionPerformed(AnActionEvent e) {
//				super.actionPerformed(e);
//				GrepProjectComponent.getInstance(myProject).unpin(file);
//			}
//		};
//		GrepProjectComponent.getInstance(myProject).register(closeAction);
//		actionGroup.add(closeAction);
//		actionGroup.add(new CloseAll());
//		return actionGroup;
//	}

	public void activateToolWindow() {
		ApplicationManager.getApplication().invokeLater(new Runnable() {
			@Override
			public void run() {
				ToolWindowManager.getInstance(myProject).getToolWindow(RunInConsoleExecutor.TOOLWINDOW_ID).activate(null);
			}
		});
	}

	public static JComponent createConsolePanel(ConsoleView view, ActionGroup actions) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(view.getComponent(), BorderLayout.CENTER);
		panel.add(createToolbar(actions, view), BorderLayout.WEST);
		return panel;
	}

	private static JComponent createToolbar(ActionGroup actions, ConsoleView view) {
		ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("GrepConsole-tail", actions, false);
		actionToolbar.setTargetComponent(view.getComponent());
		return actionToolbar.getComponent();
	}

	@Override
	public void dispose() {
	}

	static class MyImpl implements ComponentWithActions {
		private final ActionGroup myToolbar;
		private final String myToolbarPlace;
		private final JComponent myToolbarContext;
		private final JComponent mySearchComponent;
		private final JComponent myComponent;

		public MyImpl(final ActionGroup toolbar, final String toolbarPlace, final JComponent toolbarContext,
					  final JComponent searchComponent,
					  final JComponent component) {
			myToolbar = toolbar;
			myToolbarPlace = toolbarPlace;
			myToolbarContext = toolbarContext;
			mySearchComponent = searchComponent;
			myComponent = component;
		}

		@Override
		public boolean isContentBuiltIn() {
			return false;
		}

		public MyImpl(final JComponent component) {
			this(null, null, null, null, component);
		}

		@Override
		public ActionGroup getToolbarActions() {
			return myToolbar;
		}

		@Override
		public JComponent getSearchComponent() {
			return mySearchComponent;
		}

		@Override
		public String getToolbarPlace() {
			return myToolbarPlace;
		}

		@Override
		public JComponent getToolbarContextComponent() {
			return myToolbarContext;
		}

		@Override
		@NotNull
		public JComponent getComponent() {
			return myComponent;
		}
	}

}
