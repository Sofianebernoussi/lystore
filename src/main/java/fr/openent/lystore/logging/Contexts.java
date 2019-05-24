package fr.openent.lystore.logging;

public enum Contexts {
    AGENT ("AGENT"),
    SUPPLIER  ("SUPPLIER"),
    CONTRACT ("CONTRACT"),
    TAG ("TAG"),
    EQUIPMENT ("EQUIPMENT"),
    CAMPAIGN ("CAMPAIGN"),
    STRUCTUREGROUP("STRUCTUREGROUP"),
    PURSE ("PURSE"),
    BASKET("BASKET"),
    TITLE("TITLE"),
    GRADE("GRADE"),
    PROJECT("PROJECT"),
    ORDER ("ORDER"),
    OPERATION("OPERATION");

    private final String contextName;

    Contexts (String context) {
        this.contextName = context;
    }
}
