/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.planner.taskedition;
import static org.navalplanner.web.I18nHelper._;

import org.navalplanner.business.planner.entities.StartConstraintType;
import org.navalplanner.business.planner.entities.Task;
import org.navalplanner.business.planner.entities.TaskElement;
import org.navalplanner.business.planner.entities.TaskStartConstraint;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.zkoss.ganttz.TaskEditFormComposer;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.api.Combobox;
import org.zkoss.zul.api.Datebox;
import org.zkoss.zul.api.Row;

/**
 * Controller for edit {@link Task} popup.
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@org.springframework.stereotype.Component("editTaskController")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EditTaskController extends GenericForwardComposer {

    // This is a workaround, because in business we don't have access to
    // I18nHelper
    private enum WebStartConstraintType {
        AS_SOON_AS_POSSIBLE(StartConstraintType.AS_SOON_AS_POSSIBLE) {
            @Override
            public String getDescription() {
                return _("as soon as possible");
            }

            @Override
            public String getName() {
                return _("AS_SOON_AS_POSSIBLE");
            }
        },
        START_NOT_EARLIER_THAN(StartConstraintType.START_NOT_EARLIER_THAN) {
            @Override
            public String getDescription() {
                return _("start not earlier than");
            }

            @Override
            public String getName() {
                return _("START_NOT_EARLIER_THAN");
            }
        },
        START_IN_FIXED_DATE(StartConstraintType.START_IN_FIXED_DATE) {
            @Override
            public String getDescription() {
                return _("start in fixed date");
            }

            @Override
            public String getName() {
                return _("START_IN_FIXED_DATE");
            }
        };

        public static void appendItems(Combobox combo) {
            for (WebStartConstraintType type : WebStartConstraintType.values()) {
                combo.appendChild(type.createCombo());
            }
        }

        private final StartConstraintType type;

        private WebStartConstraintType(StartConstraintType type) {
            this.type = type;
        }

        public abstract String getName();

        public abstract String getDescription();

        private Comboitem createCombo() {
            Comboitem result = new Comboitem();
            result.setValue(this);
            result.setLabel(this.getName());
            result.setDescription(this.getDescription());
            return result;
        }

        public static boolean representsType(Comboitem item,
                StartConstraintType type) {
            WebStartConstraintType webType = (WebStartConstraintType) item
                    .getValue();
            return webType.equivalentTo(type);
        }

        private boolean equivalentTo(StartConstraintType type) {
            return this.type == type;
        }

        public boolean isAssociatedDateRequired() {
            return type.isAssociatedDateRequired();
        }

        public StartConstraintType getType() {
            return type;
        }
    }

    /**
     * Controller from the Gantt to manage common fields on edit {@link Task}
     * popup.
     */
    private TaskEditFormComposer taskEditFormComposer = new TaskEditFormComposer();

    private TaskElement currentTaskElement;

    private Intbox hours;

    private Intbox duration;

    private Combobox startConstraintTypes;

    private Datebox startConstraintDate;

    private Row startConstraint;

    public void showEditFormFor(Component openRelativeTo,
            org.zkoss.ganttz.data.Task task,
            TaskElement taskElement) {
        this.currentTaskElement = taskElement;
        taskEditFormComposer.showEditFormFor(openRelativeTo, task);
        updateComponentValuesForTask();
    }

    private void updateComponentValuesForTask() {
        if (currentTaskElement instanceof Task) {
            Task task = (Task) currentTaskElement;
            showDurationRow(task);
            showStartConstraintRow(task);
        } else {
            hideDurationRow();
            hideStartConstraintRow();
        }
        hours.setValue(currentTaskElement.getWorkHours());
    }

    private void hideStartConstraintRow() {
        startConstraint.setVisible(false);
    }

    private void showStartConstraintRow(Task task) {
        startConstraint.setVisible(true);
        StartConstraintType type = task.getStartConstraint()
                .getStartConstraintType();
        startConstraintTypes.setSelectedItemApi(findComboWithType(type));
        startConstraintDate.setVisible(type.isAssociatedDateRequired());
        startConstraintDate.setValue(task.getStartConstraint()
                .getConstraintDate());
    }

    private Comboitem findComboWithType(StartConstraintType type) {
        for (Object component : startConstraintTypes.getChildren()) {
            if (component instanceof Comboitem) {
                Comboitem item = (Comboitem) component;
                if (WebStartConstraintType.representsType(item, type)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void constraintTypeChoosen(WebStartConstraintType constraint) {
        startConstraintDate.setVisible(constraint.isAssociatedDateRequired());
        TaskStartConstraint taskStartConstraint = currentTaskElementAsTask()
                .getStartConstraint();
        startConstraintDate.setValue(taskStartConstraint.getConstraintDate());
    }

    private Task currentTaskElementAsTask() {
        return (Task) currentTaskElement;
    }

    private void hideDurationRow() {
        hours.getFellow("durationRow").setVisible(false);
    }

    private void showDurationRow(Task task) {
        hours.getFellow("durationRow").setVisible(true);
        duration.setValue(task.getDaysDuration());
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        taskEditFormComposer.doAfterCompose(comp);
        WebStartConstraintType.appendItems(startConstraintTypes);
        startConstraintTypes.addEventListener(Events.ON_SELECT,
                new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        WebStartConstraintType constraint = (WebStartConstraintType) startConstraintTypes
                                .getSelectedItemApi().getValue();
                        constraintTypeChoosen(constraint);
                    }
                });
    }
}
