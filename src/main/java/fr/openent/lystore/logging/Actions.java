package fr.openent.lystore.logging;

public enum Actions {
    CREATE ("CREATE"),
    UPDATE ("UPDATE"),
    DELETE ("DELETE");

    private final String actionName;

    Actions (String action) {
        this.actionName = action;
    }
}
