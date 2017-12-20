package fr.openent.lystore.logging;

public enum Contexts {
    AGENT ("AGENT"),
    SUPPLIER  ("SUPPLIER"),
    CONTRACT ("CONTRACT"),
    TAG ("TAG"),
    EQUIPMENT ("EQUIPMENT");

    private final String contextName;

    Contexts (String context) {
        this.contextName = context;
    }
}
