package gov.va.ocp.framework.service.aspect.validators;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.http.HttpStatus;

import gov.va.ocp.framework.messages.MessageKey;
import gov.va.ocp.framework.messages.MessageKeys;
import gov.va.ocp.framework.messages.MessageSeverity;
import gov.va.ocp.framework.messages.ServiceMessage;
import gov.va.ocp.framework.service.aspect.TestRequest;
import gov.va.ocp.framework.validation.AbstractStandardValidator;

/**
 * USED BY ServiceValidationAspectTest to test ServiceValidationAspect and Validator.
 *
 * @author aburkholder
 */
public class TestRequestValidator extends AbstractStandardValidator<TestRequest> {

	public static final MessageKey KEY = MessageKeys.NO_KEY;
	public static final String TEXT = "NO_KEY";
	public static final MessageSeverity SEVERITY = MessageSeverity.ERROR;
	public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

	private Method callingMethod;

	public TestRequestValidator() {
	}

	@Override
	public void validate(final TestRequest toValidate, final List<ServiceMessage> messages) {
		if ((callingMethod != null) && callingMethod.getName().contains("Sad")) {
			messages.add(new ServiceMessage(SEVERITY, STATUS, MessageKeys.NO_KEY, new Object[] {}));
		}
	}

	@Override
	public Class<TestRequest> getValidatedType() {
		return TestRequest.class;
	}

	@Override
	public void setCallingMethod(Method callingMethod) {
		this.callingMethod = callingMethod;
	}

	@Override
	public Method getCallingMethod() {
		return this.callingMethod;
	}
}
