package org.wso2.sample.user.store.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.common.AuthenticationResult;
import org.wso2.carbon.user.core.common.FailureReason;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.jdbc.UniqueIDJDBCUserStoreManager;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.sql.*;
import java.util.*;
import java.util.Date;

/* This is a Custom User Store Manager which will retrieve the authentication information from the custom database
 and do the authentication process. The best way to implement is to go through the UniqueIDJDBCUserStoreManager and overide the needed methods
 based on https://is.docs.wso2.com/en/5.11.0/setup/writing-a-custom-user-store-manager/#writing-a-custom-user-store-manager-for-a-sample-scenario.
 */

public class CustomUserStoreManager extends UniqueIDJDBCUserStoreManager {

    private static final Log log = LogFactory.getLog(CustomUserStoreManager.class);

    /*
    Constructor which will be used to register the osgi service
    */
    public CustomUserStoreManager() {
    }

    /*
    This method will get initialized when the Custom User Store Manager is loaded through the WSO2 management
    console interface.
    */
    public CustomUserStoreManager(RealmConfiguration realmConfig, Map<String, Object> properties, ClaimManager
            claimManager, ProfileConfigurationManager profileManager, UserRealm realm, Integer tenantId)
            throws UserStoreException {

        super(realmConfig, properties, claimManager, profileManager, realm, tenantId);
        if (log.isDebugEnabled()) {
            log.debug("CustomUserStoreManager initialized...");
        }
    }

    /*
    Method Used for Setting Auth Results and this is fetched from the
    UniqueIDJDBCUserStoreManager
    */
    private AuthenticationResult getAuthenticationResult(String reason) {
        AuthenticationResult authenticationResult = new AuthenticationResult(
                AuthenticationResult.AuthenticationStatus.FAIL);
        authenticationResult.setFailureReason(new FailureReason(reason));
        return authenticationResult;
    }

    /*
    Method Used for retrieving the users list from the custom user store. The users listing page in the wso2 carbon console use this method to pull the
    list of users.
    */
    @Override
    public List<User> doListUsersWithID(String filter, int maxItemLimit) throws UserStoreException {
        if (log.isDebugEnabled()) {
            log.debug("Executing doListUsersWithID:  " + filter + "maxItemLimit: " + maxItemLimit);
        }
        List<User> users = new ArrayList<>();
        Connection dbConnection = null;
        String sqlStmt;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        if (maxItemLimit == 0) {
            return Collections.emptyList();
        }

        int givenMax;
        try {
            givenMax = Integer
                    .parseInt(realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_MAX_USER_LIST));
        } catch (NumberFormatException e) {
            givenMax = UserCoreConstants.MAX_USER_ROLE_LIST;
        }

        if (maxItemLimit < 0 || maxItemLimit > givenMax) {
            maxItemLimit = givenMax;
        }

        try {

            if (filter != null && filter.trim().length() != 0) {
                filter = filter.trim();
                filter = filter.replace("*", "%");
                filter = filter.replace("?", "_"); // Used for a search like wso2?ser* in the frontend
            } else {
                filter = "%";
            }

            List<User> userList = new ArrayList<>();

            dbConnection = getDBConnection();

            if (dbConnection == null) {
                throw new UserStoreException("Attempts to establish a connection with the data source has failed.");
            }
            sqlStmt = "SELECT id,username FROM customer WHERE username LIKE ? ORDER BY username";
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, filter);

            prepStmt.setMaxRows(maxItemLimit);

            try {
                rs = prepStmt.executeQuery();
            } catch (SQLException e) {
                if (e instanceof SQLTimeoutException) {
                    log.error("The cause might be a time out. Hence ignored", e);
                    return users;
                }
                String errorMessage =
                        "Error while fetching users according to filter : " + filter + " & max Item limit " + ": "
                                + maxItemLimit;
                if (log.isDebugEnabled()) {
                    log.debug(errorMessage, e);
                }
                throw new UserStoreException(errorMessage, e);
            }

            while (rs.next()) {
                String userID = rs.getString(1);
                String userName = rs.getString(2);
                if (CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userID)) {
                    continue;
                }

                User user = getUser(userID, userName);
                userList.add(user);
            }
            rs.close();

            if (!userList.isEmpty()) {
                users = userList;
            }

        } catch (SQLException e) {
            String msg = "Error occurred while retrieving users for filter : " + filter + " & max Item limit : "
                    + maxItemLimit;
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new UserStoreException(msg, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }

        return users;
    }
    /*
    Method used when authenticating the user through Management Console and MyAccount Screen.
     */
    @Override
    protected AuthenticationResult doAuthenticateWithUserName(String userName, Object credential)
            throws UserStoreException {
        if (log.isDebugEnabled()) {
            log.debug("doAuthenticateWithUserName, Logged Username: " + userName + "Password: " + credential.toString());
        }
        AuthenticationResult authenticationResult = new AuthenticationResult(
                AuthenticationResult.AuthenticationStatus.FAIL);
        User user;

        if (!isValidUserName(userName)) {
            String reason = "Username validation failed.";
            if (log.isDebugEnabled()) {
                log.debug(reason);
            }
            return getAuthenticationResult(reason);
        }

        if (UserCoreUtil.isRegistryAnnonymousUser(userName)) {
            String reason = "Anonymous user trying to login.";
            log.error(reason);
            return getAuthenticationResult(reason);
        }

        if (!isValidCredentials(credential)) {
            String reason = "Password validation failed.";
            if (log.isDebugEnabled()) {
                log.debug(reason);
            }
            return getAuthenticationResult(reason);
        }

        Connection dbConnection = null;
        ResultSet rs = null;
        PreparedStatement prepStmt = null;
        String sqlstmt;
        String password = null;
        boolean isAuthed = false;

        try {
            dbConnection = getDBConnection();
            dbConnection.setAutoCommit(false);

            sqlstmt = "SELECT id,username,password FROM customer WHERE username=?";

            if (log.isDebugEnabled()) {
                log.debug(sqlstmt);
            }

            prepStmt = dbConnection.prepareStatement(sqlstmt);
            prepStmt.setString(1, userName);

            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String userID = rs.getString(1);
                String storedPassword = rs.getString(3);

                password = credential.toString();

                if ((storedPassword != null) && (storedPassword.equals(password))) {
                    isAuthed = true;
                    user = getUser(userID, userName);
                    authenticationResult = new AuthenticationResult(
                            AuthenticationResult.AuthenticationStatus.SUCCESS);
                    authenticationResult.setAuthenticatedUser(user);
                }
            }
        } catch (SQLException e) {
            String msg = "Error occurred while retrieving user authentication info for userName : " + userName;
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new UserStoreException("Authentication Failure", e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }
        if (log.isDebugEnabled()) {
            log.debug("UserName " + userName + " login attempt. Login success: " + isAuthed);
        }
        return authenticationResult;
    }

    /*
    This is needed to set the Mandatory, Optional and Advanced Properties at the User interface of the Custom User Store Page
    where we add the Custom User Store.
     */
    @Override
    public Properties getDefaultUserStoreProperties() {
        Properties properties = new Properties();
        properties.setMandatoryProperties(CustomUserStoreConstants.CUSTOM_UM_MANDATORY_PROPERTIES.toArray
                (new Property[0]));
        properties.setOptionalProperties(CustomUserStoreConstants.CUSTOM_UM_OPTIONAL_PROPERTIES.toArray
                (new Property[0]));
        properties.setAdvancedProperties(CustomUserStoreConstants.CUSTOM_UM_ADVANCED_PROPERTIES.toArray
                (new Property[0]));
        return properties;
    }

    /*
    This method is needed when the user listing happens, otherwise it will throw error at console
     */
    @Override
    protected String doGetUserIDFromUserNameWithID(String userName) throws UserStoreException {

        if (userName == null) {
            throw new IllegalArgumentException("userName cannot be null.");
        }

        Connection dbConnection = null;
        String sqlStmt;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String userID = null;
        try {
            dbConnection = getDBConnection();

            sqlStmt = "SELECT id FROM customer WHERE username=?";
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, userName);

            rs = prepStmt.executeQuery();
            while (rs.next()) {
                userID = rs.getString(1);
            }
        } catch (SQLException e) {
            String msg = "Database error occurred while retrieving userID for a UserName : " + userName;
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            }
            throw new UserStoreException(msg, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, prepStmt);
        }

        return userID;
    }

    /*
    While logging in it is needed to get the Password Expiry time and this method is overidden for testing purposes
    for correct implementation this needs be pulled from the exact database table column value.
    */
    public Date doGetPasswordExpirationTimeWithID(String userID) throws UserStoreException {
        log.info("Retrieving the password Expiry Date...");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 1);

        log.info("Password Expiry: " + calendar.getTime());

        return calendar.getTime();
    }

}
