/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 27/nov/2012
 * Copyright 2012 by Andrea Vacondio (andrea.vacondio@gmail.com).
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
package org.pdfsam.task;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.sejda.eventstudio.StaticStudio.eventStudio;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.pdfsam.module.TaskExecutionRequestEvent;
import org.pdfsam.module.UsageService;
import org.sejda.core.notification.context.GlobalNotificationContext;
import org.sejda.core.service.TaskExecutionService;
import org.sejda.eventstudio.annotation.EventListener;
import org.sejda.model.notification.event.AbstractNotificationEvent;
import org.sejda.model.notification.event.PercentageOfWorkDoneChangedEvent;
import org.sejda.model.notification.event.TaskExecutionCompletedEvent;
import org.sejda.model.notification.event.TaskExecutionFailedEvent;
import org.sejda.model.notification.event.TaskExecutionStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

/**
 * Component listening for {@link TaskExecutionRequestEvent} and triggering the actual execution
 * 
 * @author Andrea Vacondio
 * 
 */
@Named
class TaskExecutionController implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(TaskExecutionController.class);

    private TaskExecutionService executionService;
    private UsageService usageService;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String currentModule = StringUtils.EMPTY;

    @Inject
    public TaskExecutionController(TaskExecutionService executionService, UsageService usageService) {
        this.executionService = executionService;
        this.usageService = usageService;
        eventStudio().addAnnotatedListeners(this);
        GlobalNotificationContext.getContext().addListener(TaskExecutionFailedEvent.class,
                new TaskEventBroadcaster<TaskExecutionFailedEvent>());
        GlobalNotificationContext.getContext().addListener(TaskExecutionStartedEvent.class,
                new TaskEventBroadcaster<TaskExecutionStartedEvent>());
        GlobalNotificationContext.getContext().addListener(TaskExecutionCompletedEvent.class,
                new TaskEventBroadcaster<TaskExecutionCompletedEvent>());
        GlobalNotificationContext.getContext().addListener(PercentageOfWorkDoneChangedEvent.class,
                new TaskEventBroadcaster<PercentageOfWorkDoneChangedEvent>());
    }

    /**
     * Request a task execution
     * 
     * @param event
     */
    @EventListener(priority = Integer.MAX_VALUE)
    public void request(TaskExecutionRequestEvent event) {
        LOG.trace("Task execution request received");
        usageService.incrementUsageFor(event.getModuleId());
        currentModule = event.getModuleId();
        executor.execute(() -> executionService.execute(event.getParameters()));
        LOG.trace("Task execution submitted");
    }

    @Override
    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }

    class TaskEventBroadcaster<T extends AbstractNotificationEvent>
            implements org.sejda.model.notification.EventListener<T> {

        @Override
        public void onEvent(T event) {
            Platform.runLater(() -> eventStudio().broadcast(event));
            if (isNoneBlank(currentModule)) {
                Platform.runLater(() -> eventStudio().broadcast(event, currentModule));
            }
        }
    }
}
