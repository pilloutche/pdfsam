/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 21/mar/2014
 * Copyright 2013-2014 by Andrea Vacondio (andrea.vacondio@gmail.com).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.ui.module;

import static org.sejda.eventstudio.StaticStudio.eventStudio;

import java.math.BigDecimal;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.pdfsam.context.DefaultI18nContext;
import org.pdfsam.module.TaskExecutionRequestEvent;
import org.pdfsam.ui.support.ShowRequestEvent;
import org.sejda.eventstudio.annotation.EventListener;
import org.sejda.model.exception.TaskOutputVisitException;
import org.sejda.model.notification.event.PercentageOfWorkDoneChangedEvent;
import org.sejda.model.notification.event.TaskExecutionCompletedEvent;
import org.sejda.model.notification.event.TaskExecutionFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import de.jensd.fx.fontawesome.AwesomeIcon;

/**
 * Footer common to all the modules that include the run button and the progress bar.
 * 
 * @author Andrea Vacondio
 *
 */
@Named
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ModuleFooterPane extends HBox {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleFooterPane.class);

    private RunButton runButton = new RunButton();
    private Button failed = new Button();
    private OpenButton open = new OpenButton();
    private ProgressBar bar = new ProgressBar(0);

    public ModuleFooterPane() {
        this.getStyleClass().add("task-footer");
        Label failedGraphics = new Label(AwesomeIcon.WARNING.toString());
        failedGraphics.getStyleClass().add("task-failed");
        failed.getStyleClass().addAll("pdfsam-toolbar-button", "task-status-button");
        failed.setGraphic(failedGraphics);
        failed.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        failed.setVisible(false);
        failed.setOnAction(e -> eventStudio().broadcast(new ShowRequestEvent(), "LogStage"));
        failed.setTooltip(new Tooltip(DefaultI18nContext.getInstance().i18n("Task execution failed")));
        open.setVisible(false);
        bar.setRotate(180);
        bar.setPrefWidth(280);
    }

    @PostConstruct
    void init() {
        StackPane buttons = new StackPane(failed, open);
        buttons.getStyleClass().add("progress-icons");
        this.getChildren().addAll(bar, buttons, runButton);
        eventStudio().addAnnotatedListeners(this);
    }

    @EventListener
    public void onTaskExecutionRequest(TaskExecutionRequestEvent event) {
        open.setVisible(false);
        failed.setVisible(false);
        bar.setProgress(0);
        try {
            if (event.getParameters().getOutput() != null) {
                event.getParameters().getOutput().accept(open);
            }
        } catch (TaskOutputVisitException e) {
            LOG.warn("This should never happen", e);
        }
    }

    @EventListener
    public void onTaskCompleted(TaskExecutionCompletedEvent event) {
        open.setVisible(true);
        failed.setVisible(false);
        bar.setProgress(1);
    }

    @EventListener
    public void onTaskFailed(TaskExecutionFailedEvent event) {
        open.setVisible(false);
        failed.setVisible(true);
        bar.setProgress(1);
    }

    @EventListener
    public void onProgress(PercentageOfWorkDoneChangedEvent event) {
        open.setVisible(false);
        failed.setVisible(false);
        if (event.isUndetermined()) {
            bar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        } else {
            bar.setProgress(event.getPercentage().divide(new BigDecimal(100)).doubleValue());
        }
    }

    RunButton runButton() {
        return runButton;
    }
}