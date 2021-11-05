package io.sapl.test.unit.usecase;

import static io.sapl.hamcrest.Matchers.hasObligationContainingKeyValue;
import static io.sapl.hamcrest.Matchers.hasObligationMatching;
import static io.sapl.hamcrest.Matchers.isPermit;
import static io.sapl.hamcrest.Matchers.isResourceMatching;
import static org.hamcrest.CoreMatchers.allOf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.api.pdp.Decision;
import io.sapl.functions.FilterFunctionLibrary;
import io.sapl.interpreter.InitializationException;
import io.sapl.test.SaplTestFixture;
import io.sapl.test.unit.SaplUnitTestFixture;

public class G_PolicyWithComplexExpectStepTest {
	
	private SaplTestFixture fixture;
	private ObjectMapper mapper;
	
	
	static class SubjectDTO {
		public String name = "Willi";
		public String authority = "ROLE_ADMIN";
	}
	private final Object subject = new SubjectDTO();
	
	static class ActionDTO {
		static class JavaDTO {
			public String name = "findById";
		}
		public JavaDTO java = new JavaDTO();
	}

	private final Object action = new ActionDTO();
	
	static class ResourceDTO {
		public String id = "56";
		public String diagnosisText = "diagnosisText";
		public String icd11Code = "icd11Code";
	}

	private final Object resource = new ResourceDTO();

	@BeforeEach
	public void setUp() throws InitializationException {
		fixture = new SaplUnitTestFixture("policyWithObligationAndResource.sapl")
				.registerFunctionLibrary(new FilterFunctionLibrary());
		mapper = new ObjectMapper();
	}
	
	
	@Test
	void test_equals() {
		ObjectNode obligation = mapper.createObjectNode();
		obligation.put("type", "logAccess");
		obligation.put("message", "Willi has accessed patient data (id=56) as an administrator.");
		ArrayNode obligations = mapper.createArrayNode();
		obligations.add(obligation);
		
		ObjectNode resource = mapper.createObjectNode();
		resource.put("id", "56");
		resource.put("diagnosisText", "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588");
		resource.put("icd11Code", "ic\u2588\u2588\u2588\u2588\u2588\u2588\u2588");
		
		AuthorizationDecision decision = new AuthorizationDecision(Decision.PERMIT).withObligations(obligations).withResource(resource);
			
		fixture.constructTestCase()
			.when(AuthorizationSubscription.of(subject, action, resource))
			.expect(decision)
			.verify();
		
	}
	
	
	@Test
	void test_equalsPredicate() {
		fixture.constructTestCase()
		.when(AuthorizationSubscription.of(subject, action, resource))
			.expect((AuthorizationDecision dec) -> {
				
				if(dec.getDecision() != Decision.PERMIT) {
					return false;
				}
				
				if(dec.getObligations().isEmpty() || dec.getResource().isEmpty()) {
					return false;
				}
				
				//check obligation
				boolean containsExpectedObligation = false;
				for (JsonNode node : dec.getObligations().get()) {
					if(node.has("type") && node.get("type").asText().equals("logAccess") 
							&& node.has("message") && node.get("message").asText().equals("Willi has accessed patient data (id=56) as an administrator.")) {
						containsExpectedObligation = true;
					}
				}
				
				
				//check resource
				boolean containsExpectedResource = false;
				JsonNode resource = dec.getResource().get();
				if(resource.has("id") && resource.get("id").asText().equals("56")
						&& resource.has("diagnosisText") && resource.get("diagnosisText").asText().equals("\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588")
						&& resource.has("icd11Code") && resource.get("icd11Code").asText().equals("ic\u2588\u2588\u2588\u2588\u2588\u2588\u2588")) {
					containsExpectedResource = true;
				}
				
				return containsExpectedObligation && containsExpectedResource;
			})
			.verify();
		
	}
	
	
	
	@Test
	void test_equalsMatcher() {
		fixture.constructTestCase()
		.when(AuthorizationSubscription.of(subject, action, resource))
			.expect(
				allOf(
					isPermit(),
					
					//check Obligations
					// via .equals()
					////hasObligation(mapper.createObjectNode().put("foo", "bar")),
					// or Predicate
					hasObligationMatching((JsonNode obligation) -> {
						return obligation.has("type") && obligation.get("type").asText().equals("logAccess")
								&& obligation.has("message") && obligation.get("message").asText().equals("Willi has accessed patient data (id=56) as an administrator.");
					}),
					
					hasObligationContainingKeyValue("type", "logAccess"),
					
					
					//check advice
					// via .equals()					
					////hasAdvice(mapper.createObjectNode().put("foo", "bar")),
					//or Predicate
					////hasAdviceMatching((JsonNode advice) -> {
					////	return advice.has("sendEmail");
					////}),
					
					//check Resource
					// via .equals()	
					////isResourceEquals(new ObjectMapper().createObjectNode().put("foo", "bar")), 
					//or Predicate
					isResourceMatching((JsonNode resource) -> resource.has("id") && resource.get("id").asText().equals("56")
							&& resource.has("diagnosisText") && resource.get("diagnosisText").asText().equals("\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588")
							&& resource.has("icd11Code") && resource.get("icd11Code").asText().equals("ic\u2588\u2588\u2588\u2588\u2588\u2588\u2588"))
				)
			)
			.verify();
		
	}
}
