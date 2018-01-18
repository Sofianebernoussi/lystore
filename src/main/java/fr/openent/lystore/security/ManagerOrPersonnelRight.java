package fr.openent.lystore.security;

import fr.openent.lystore.security.WorkflowActionUtils;
import fr.openent.lystore.security.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Created by rahnir on 17/01/2018.
 */
public class ManagerOrPersonnelRight implements ResourcesProvider {
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.MANAGER_RIGHT.toString()) || user.getType().equals("Personnel") );
    }

}
