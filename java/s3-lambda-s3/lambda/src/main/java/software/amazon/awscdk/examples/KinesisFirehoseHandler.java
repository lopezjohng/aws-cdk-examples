package software.amazon.awscdk.examples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.apache.http.HttpStatus;

public class KinesisFirehoseHandler implements RequestHandler<APIGatewayV2HTTPEvent, String> {
  private AmazonSNS snsClient;

  public KinesisFirehoseHandler() {
    //AmazonSNSClient snsClient = AmazonSNSClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    snsClient = AmazonSNSClient.builder().withRegion(Regions.US_EAST_1).build();
  }

  @Override
  public String handleRequest(APIGatewayV2HTTPEvent apiGatewayV2HTTPEvent, Context context) {
    try {
      String accountNumber = apiGatewayV2HTTPEvent.getPathParameters().get("accountNumber");
      String topicArn = System.getenv("SNS_TOPIC_ARN");
      snsClient.publish(topicArn, accountNumber);

      return Integer.toString(HttpStatus.SC_ACCEPTED);
    }
    catch (Exception ex) {
      return ex.getMessage();
    }
  }
}
