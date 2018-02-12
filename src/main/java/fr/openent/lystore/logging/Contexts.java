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
    ORDER ("ORDER");

    private final String contextName;

    Contexts (String context) {
        this.contextName = context;
    }
}
