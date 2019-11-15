###Purpose:

To delete a user from AWS cognito pool, given its user pool name and user phone number.

####To build:

./gradlew clean shadowJar

####To run:

AWS_ACCESS_KEY_ID="xxx" AWS_SECRET_ACCESS_KEY="xxx" AWS_DEFAULT_REGION="xxx" poolName="XXX" phoneNumber="XXX" java -jar build/libs/aws-cognito-delete-user-0.1-all.jar

#####PS: No exception handling and no unit tests. This is a quick hack for enabling an integration test in another project.