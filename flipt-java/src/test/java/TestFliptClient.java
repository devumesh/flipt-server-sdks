import io.flipt.api.FliptClient;
import io.flipt.api.authentication.AuthenticationStrategy;
import io.flipt.api.authentication.ClientTokenAuthenticationStrategy;
import io.flipt.api.evaluation.models.*;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestFliptClient {
  @Test
  void testVariantEvaluation() {
    String fliptURL = System.getenv().get("FLIPT_URL");
    String authToken = System.getenv().get("FLIPT_AUTH_TOKEN");

    assert fliptURL != null && !fliptURL.isEmpty();
    assert authToken != null && !authToken.isEmpty();

    AuthenticationStrategy authenticationStrategy =
        new ClientTokenAuthenticationStrategy(authToken);

    Map<String, String> headers = Map.of("Accept", "application/json");

    FliptClient fc =
        FliptClient.builder()
            .url(fliptURL)
            .authentication(authenticationStrategy)
            .headers(headers)
            .build();

    Map<String, String> context = new HashMap<>();
    context.put("fizz", "buzz");

    EvaluationRequest req =
        new EvaluationRequest("default", "flag1", "entity", context, Optional.empty());

    VariantEvaluationResponse variant =
        Assertions.assertDoesNotThrow(() -> fc.evaluation().evaluateVariant(req));
    Assertions.assertTrue(variant.isMatch());
    Assertions.assertEquals("flag1", variant.getFlagKey());
    Assertions.assertEquals("MATCH_EVALUATION_REASON", variant.getReason().toString());
    Assertions.assertEquals("variant1", variant.getVariantKey());
    Assertions.assertEquals("segment1", variant.getSegmentKeys().get(0));

    Assertions.assertEquals("variant1", fc.evaluation().variantValue(req, "fallback"));

    Assertions.assertEquals(
        "fallback",
        fc.evaluation()
            .variantValue(
                new EvaluationRequest("default", "flag-none", "entity", context, Optional.empty()),
                "fallback"));
  }

  @Test
  void testBooleanEvaluation() {
    String fliptURL = System.getenv().get("FLIPT_URL");
    String authToken = System.getenv().get("FLIPT_AUTH_TOKEN");

    assert fliptURL != null && !fliptURL.isEmpty();
    assert authToken != null && !authToken.isEmpty();

    AuthenticationStrategy authenticationStrategy =
        new ClientTokenAuthenticationStrategy(authToken);

    Map<String, String> headers = Map.of("Accept", "application/json");

    FliptClient fc =
        FliptClient.builder()
            .url(fliptURL)
            .authentication(authenticationStrategy)
            .headers(headers)
            .build();

    Map<String, String> context = new HashMap<>();
    context.put("fizz", "buzz");

    EvaluationRequest req =
        new EvaluationRequest("default", "flag_boolean", "entity", context, Optional.empty());
    BooleanEvaluationResponse booleanEvaluation =
        Assertions.assertDoesNotThrow(() -> fc.evaluation().evaluateBoolean(req));

    Assertions.assertTrue(booleanEvaluation.isEnabled());
    Assertions.assertEquals("flag_boolean", booleanEvaluation.getFlagKey());
    Assertions.assertEquals("MATCH_EVALUATION_REASON", booleanEvaluation.getReason().toString());

    Assertions.assertTrue(fc.evaluation().booleanValue(req, false));
    Assertions.assertTrue(
        fc.evaluation()
            .booleanValue(
                new EvaluationRequest("default", "flag-none", "entity", context, Optional.empty()),
                true));
  }

  @Test
  void testBatchEvaluation() {
    String fliptURL = System.getenv().get("FLIPT_URL");
    String authToken = System.getenv().get("FLIPT_AUTH_TOKEN");

    assert fliptURL != null && !fliptURL.isEmpty();
    assert authToken != null && !authToken.isEmpty();

    AuthenticationStrategy authenticationStrategy =
        new ClientTokenAuthenticationStrategy(authToken);

    Map<String, String> headers = Map.of("Accept", "application/json");

    FliptClient fc =
        FliptClient.builder()
            .url(fliptURL)
            .authentication(authenticationStrategy)
            .headers(headers)
            .build();

    Map<String, String> context = new HashMap<>();
    context.put("fizz", "buzz");

    EvaluationRequest variantEvaluationRequest =
        new EvaluationRequest("default", "flag1", "entity", context, Optional.empty());
    EvaluationRequest booleanEvaluationRequest =
        new EvaluationRequest("default", "flag_boolean", "entity", context, Optional.empty());
    EvaluationRequest errorEvaluationRequest =
        new EvaluationRequest("default", "flag1234", "entity", new HashMap<>(), Optional.empty());

    List<EvaluationRequest> evaluationRequests = new ArrayList<>();
    evaluationRequests.add(variantEvaluationRequest);
    evaluationRequests.add(booleanEvaluationRequest);
    evaluationRequests.add(errorEvaluationRequest);

    BatchEvaluationResponse batch =
        Assertions.assertDoesNotThrow(
            () ->
                fc.evaluation()
                    .evaluateBatch(
                        new BatchEvaluationRequest(
                            Optional.of(""), evaluationRequests, Optional.empty())));

    // Variant
    EvaluationResponse first = batch.getResponses().get(0);
    Assertions.assertEquals("VARIANT_EVALUATION_RESPONSE_TYPE", first.getType().toString());

    VariantEvaluationResponse variant = first.getVariantResponse().get();
    Assertions.assertTrue(variant.isMatch());
    Assertions.assertEquals("flag1", variant.getFlagKey());
    Assertions.assertEquals("MATCH_EVALUATION_REASON", variant.getReason().toString());
    Assertions.assertEquals("variant1", variant.getVariantKey());
    Assertions.assertEquals("segment1", variant.getSegmentKeys().get(0));

    // Boolean
    EvaluationResponse second = batch.getResponses().get(1);
    Assertions.assertEquals("BOOLEAN_EVALUATION_RESPONSE_TYPE", second.getType().toString());

    BooleanEvaluationResponse booleanEvaluation = second.getBooleanResponse().get();
    Assertions.assertTrue(booleanEvaluation.isEnabled());
    Assertions.assertEquals("flag_boolean", booleanEvaluation.getFlagKey());
    Assertions.assertEquals("MATCH_EVALUATION_REASON", booleanEvaluation.getReason().toString());

    // Error
    EvaluationResponse third = batch.getResponses().get(2);
    Assertions.assertEquals("ERROR_EVALUATION_RESPONSE_TYPE", third.getType().toString());

    ErrorEvaluationResponse errorEvaluation = third.getErrorResponse().get();
    Assertions.assertEquals("flag1234", errorEvaluation.getFlagKey());
    Assertions.assertEquals("default", errorEvaluation.getNamespaceKey());
    Assertions.assertEquals(
        "NOT_FOUND_ERROR_EVALUATION_REASON", errorEvaluation.getReason().toString());
  }
}
