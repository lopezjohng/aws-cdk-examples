package software.amazon.awscdk.examples;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.events.targets.KinesisFirehoseStream;
import software.amazon.awscdk.services.events.targets.KinesisStream;
import software.amazon.awscdk.services.kinesis.Stream;
import software.amazon.awscdk.services.kinesisfirehose.CfnDeliveryStream;
import software.constructs.Construct;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;
import software.amazon.awscdk.services.lambda.Runtime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static software.amazon.awscdk.services.s3.EventType.OBJECT_CREATED;


/** Hello, CDK for Java! */
class S3LambdaStack extends Stack {

  static final String sourceBucket = "testcdk-s3-notifications-lambda";
  static final String targetBucket = "testcdk-s3-notifications-lambda-target";
  public S3LambdaStack(final Construct parent, final String name) {
    super(parent, name);

    new S3NotificationsLambda(this,"testS3");
  }



  static class S3NotificationsLambda extends Construct {
    S3NotificationsLambda(
      final Construct parent, final String name) {
      super(parent, name);

      //create destination bucket
      Bucket bucketName = Bucket.Builder.create(this,targetBucket).build();

      //create lambda, roles and permissions
      PolicyStatement statement1 = PolicyStatement.Builder.create()
        .effect(Effect.ALLOW)
        .actions(Arrays.asList(new String[] {"s3:GetBucket","s3:PutObject"}))
        .resources(Arrays.asList(new String[] {"arn:aws:s3:::"+bucketName.getBucketName()+"/*"})).build();

      PolicyStatement statement2 = PolicyStatement.Builder.create()
        .effect(Effect.ALLOW)
        .actions(Arrays.asList(new String[] {"logs:CreateLogGroup","logs:CreateLogStream","logs:PutLogEvents"}))
        .resources(Arrays.asList(new String[] {"arn:aws:logs:*:*:*"})).build();

      PolicyDocument policyDocument = PolicyDocument.Builder.create()
        .statements(Arrays.asList(new PolicyStatement[]{statement1,statement2})).build();




      Role lambdaRole = Role.Builder.create(this,"LambdaIAMRole")
        .roleName("LambdaIAMRole")
        .inlinePolicies(Collections.singletonMap("key", policyDocument))
        .path("/")
        .assumedBy(new ServicePrincipal("lambda.amazonaws.com")).build();

      // Create DynamoDB table
      TableProps tableProps;
      Attribute partitionKey = Attribute.builder()
        .name("itemId")
        .type(AttributeType.STRING)
        .build();
      Stream stream = new Stream(this, "Stream");
      Table dynamodbTable = Table.Builder.create(this, "items")
        .partitionKey(partitionKey)
        .removalPolicy(RemovalPolicy.DESTROY)
        .kinesisStream(stream)
        .build();
      Map<String, String> lambdaEnvMap = new HashMap<>();
      lambdaEnvMap.put("TABLE_NAME", dynamodbTable.getTableName());
      lambdaEnvMap.put("PRIMARY_KEY","itemId");

      Function lambda = Function.Builder.create(this,"HelloLambda")
        .code(Code.fromAsset("./asset/lambda-1.0.0-jar-with-dependencies.jar"))
        .handler("software.amazon.awscdk.examples.S3EventHandler")
        .role(lambdaRole)
        .runtime(Runtime.JAVA_8).memorySize(1024)
        .environment(lambdaEnvMap)
        .timeout(Duration.minutes(5)).build();

      dynamodbTable.grantReadWriteData(lambda);

      //create s3 notification bucket
      Bucket s3 = Bucket.Builder.create(this,sourceBucket).build();

      CfnDeliveryStream cfnDeliveryStream = CfnDeliveryStream.Builder.create(this, "DeliveryStream").
      KinesisFirehoseStream.Builder.create(cfnDeliveryStream).

      PolicyStatement statement3 = PolicyStatement.Builder.create()
        .effect(Effect.ALLOW)
        .actions(Arrays.asList(new String[] {"s3:GetObject","s3:PutObject"}))
        .resources(Arrays.asList(new String[] {"arn:aws:s3:::"+s3.getBucketName()+"/*"})).build();
      lambda.getRole().attachInlinePolicy(new Policy(this,"s3-bucket-policy",
        PolicyProps.builder().statements(Arrays.asList( new PolicyStatement[]{statement3})).build()));
      lambda.addEnvironment("target",bucketName.getBucketName());

      //configure s3 notifications
      LambdaDestination functionDestination = new LambdaDestination(lambda);
      s3.addEventNotification(OBJECT_CREATED,functionDestination);

      // Configure table stream

      // Create Kinesis

      // Create SNS topic


    }
  }
}
