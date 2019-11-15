package com.happysathya.aws.cognito;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserResult;
import com.amazonaws.services.cognitoidp.model.ListUserPoolsRequest;
import com.amazonaws.services.cognitoidp.model.ListUserPoolsResult;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.UserPoolDescriptionType;
import com.amazonaws.services.cognitoidp.model.UserType;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DeleteUserByPhoneNumber {

    public static void main(String[] args) {

        AWSCognitoIdentityProvider identityProvider = getAwsCognitoIdentityProvider();
        String poolName = getProperty("poolName");
        String phoneNumber = getProperty("phoneNumber");

        Optional<UserPoolDescriptionType> userPool = findUserPool(identityProvider, poolName, null);
        if (userPool.isEmpty())
            throw new RuntimeException(String.format("Sorry, unable to find %s", poolName));

        userPool.ifPresent(userPoolDescriptionType -> {
            Optional<UserType> user = findUser(identityProvider, phoneNumber, userPoolDescriptionType);
            if (user.isEmpty())
                throw new RuntimeException(String.format("Sorry, unable to find %s in %s", phoneNumber, poolName));
            deleteUser(identityProvider, userPoolDescriptionType, user.get());
        });


    }

    private static AdminDeleteUserResult deleteUser(AWSCognitoIdentityProvider identityProvider,
                                                    UserPoolDescriptionType userPoolDescriptionType,
                                                    UserType userType) {
        AdminDeleteUserRequest adminDeleteUserRequest = new AdminDeleteUserRequest()
                .withUsername(userType.getUsername())
                .withUserPoolId(userPoolDescriptionType.getId());
        return identityProvider.adminDeleteUser(adminDeleteUserRequest);
    }

    private static Optional<UserType> findUser(AWSCognitoIdentityProvider identityProvider,
                                               String phoneNumber,
                                               UserPoolDescriptionType userPoolDescriptionType) {
        ListUsersRequest listUsersRequest = new ListUsersRequest()
                .withFilter(String.format("phone_number = \"%s\"", phoneNumber))
                .withUserPoolId(userPoolDescriptionType.getId());
        return identityProvider.listUsers(listUsersRequest).getUsers().stream().findFirst();
    }

    //recursion
    private static Optional<UserPoolDescriptionType> findUserPool(AWSCognitoIdentityProvider identityProvider,
                                                                  String userPoolName,
                                                                  String searchToken) {
        ListUserPoolsRequest listUserPoolsRequest = new ListUserPoolsRequest()
                .withMaxResults(25)
                .withNextToken(searchToken);
        Predicate<UserPoolDescriptionType> searchPredicate = userPoolDescriptionType -> userPoolDescriptionType.getName()
                .equals(userPoolName);
        ListUserPoolsResult listUserPoolsResult = identityProvider.listUserPools(listUserPoolsRequest);
        Optional<UserPoolDescriptionType> result = listUserPoolsResult
                .getUserPools().stream()
                .filter(searchPredicate)
                .findFirst();

        if (result.isPresent())
            return result;
        Optional<String> nextToken = Optional.ofNullable(listUserPoolsResult.getNextToken());
        return nextToken.isEmpty() ? Optional.empty() : findUserPool(identityProvider, userPoolName, nextToken.get());
    }

    private static String getProperty(String propertyName) {
        return Optional.ofNullable(System.getenv(propertyName))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Property name %s is not present", propertyName)));
    }

    private static AWSCognitoIdentityProvider getAwsCognitoIdentityProvider() {
        Optional<String> accessKeyId = Optional.ofNullable(System.getenv("AWS_ACCESS_KEY_ID"));
        Optional<String> secretKey = Optional.ofNullable(System.getenv("AWS_SECRET_ACCESS_KEY"));
        Optional<String> region = Optional.ofNullable(System.getenv("AWS_DEFAULT_REGION"));

        if (!Stream.of(accessKeyId, secretKey, region).allMatch(Optional::isPresent)) {
            throw new IllegalArgumentException("AWS environment variables not present");
        }

        return AWSCognitoIdentityProviderClient.builder()
                .withCredentials(new AWSCredentialsProvider() {
                    @Override
                    public AWSCredentials getCredentials() {
                        return new BasicAWSCredentials(accessKeyId.get(), secretKey.get());
                    }

                    @Override
                    public void refresh() {

                    }
                })
                .withRegion(region.get())
                .build();
    }

}
