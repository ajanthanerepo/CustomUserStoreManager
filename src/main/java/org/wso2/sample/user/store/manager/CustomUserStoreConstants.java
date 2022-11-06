package org.wso2.sample.user.store.manager;

import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.core.UserStoreConfigConstants;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;

import java.util.ArrayList;
import java.util.List;

/*
This is needed to set the Mandatory, Optional and Advanced Properties at the User interface of the Custom User Store Page
where we add the Custom User Store.
*/
public class CustomUserStoreConstants {

    public static final List<Property> CUSTOM_UM_MANDATORY_PROPERTIES = new ArrayList<Property>();
    public static final List<Property> CUSTOM_UM_OPTIONAL_PROPERTIES = new ArrayList<Property>();
    public static final List<Property> CUSTOM_UM_ADVANCED_PROPERTIES = new ArrayList<Property>();

    static {
        setMandatoryProperty(JDBCRealmConstants.DRIVER_NAME, "Custom Driver Name", "",
                "Full qualified driver name");
        setMandatoryProperty(JDBCRealmConstants.URL, "Custom Connection URL", "",
                "URL of the user store database");
        setMandatoryProperty(JDBCRealmConstants.USER_NAME, "Custom User Name", "",
                "Username for the database");
        setMandatoryProperty(JDBCRealmConstants.PASSWORD, "Custom Password", "",
                "Password for the database");
        setProperty(UserStoreConfigConstants.disabled, "Disabled", "false",
                UserStoreConfigConstants.disabledDescription);
        setProperty("ReadOnly", "Read Only", "true",
                "Indicates whether the user store of this realm operates in the user read only mode or not");
        setAdvancedProperty("SelectUserByUsername", "Select User By Username SQL",
                "SELECT id,username,password FROM customer WHERE username=?", "");
        setAdvancedProperty("UserFilterByUsernameSQL", "User Filter By Username SQL",
                "SELECT id,username FROM customer WHERE username LIKE ? ORDER BY username", "");
        setAdvancedProperty("SelectUserIDByUsername", "Select User ID By Username SQL",
                "SELECT id FROM customer WHERE username=?", "");
    }

    private static void setProperty(String name, String displayName, String value, String description) {

        Property property = new Property(name, value, displayName + "#" + description, null);
        CUSTOM_UM_OPTIONAL_PROPERTIES.add(property);
    }

    private static void setMandatoryProperty(String name, String displayName, String value, String description) {

        Property property = new Property(name, value, displayName + "#" + description, null);
        CUSTOM_UM_MANDATORY_PROPERTIES.add(property);
    }

    private static void setAdvancedProperty(String name, String displayName, String value, String description) {

        Property property = new Property(name, value, displayName + "#" + description, null);
        CUSTOM_UM_ADVANCED_PROPERTIES.add(property);
    }
}
