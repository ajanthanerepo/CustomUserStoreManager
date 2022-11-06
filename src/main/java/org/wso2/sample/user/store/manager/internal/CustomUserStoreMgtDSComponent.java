package org.wso2.sample.user.store.manager.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.sample.user.store.manager.CustomUserStoreManager;


@Component(
        name = "org.wso2.sample.user.store.manager.dscomponent",
        immediate = true
)
public class CustomUserStoreMgtDSComponent {
    private static Log log = LogFactory.getLog(CustomUserStoreMgtDSComponent.class);
    private static RealmService realmService;

    /*
    OSGI Bundle activation where the Custom User Store will be registered and activated on the WSO2 Identity Server
     */
    @Activate
    protected void activate(ComponentContext ctxt) {
        try {
            CustomUserStoreManager customUserStoreManager = new CustomUserStoreManager();
            ctxt.getBundleContext().registerService(UserStoreManager.class.getName(), customUserStoreManager, null);
            log.info("CustomUserStoreManager bundle activated successfully..");
        } catch (Throwable storeError) {
            log.error("ERROR when activating Custom User Store", storeError);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        log.info("Custom User Store Manager is deactivated... ");
    }

    /* This is WSO2 related realm service where all the user related configuration combined and add here. more information can be found at
    https://is.docs.wso2.com/en/latest/deploy/configure-the-realm/
    */
    @Reference(
            name = "RealmService",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        realmService = null;
    }
}
