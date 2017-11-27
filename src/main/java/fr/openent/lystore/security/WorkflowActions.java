package fr.openent.lystore.security;

import fr.openent.lystore.Lystore;

public enum WorkflowActions {
    ADMINISTRATOR_RIGHT (Lystore.ADMINISTRATOR_RIGHT),
    MANAGER_RIGHT (Lystore.MANAGER_RIGHT);

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
