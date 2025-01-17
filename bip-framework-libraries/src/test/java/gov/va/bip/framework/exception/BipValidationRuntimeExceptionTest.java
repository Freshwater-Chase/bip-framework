package gov.va.bip.framework.exception;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import gov.va.bip.framework.exception.BipValidationRuntimeException;
import gov.va.bip.framework.messages.MessageKey;
import gov.va.bip.framework.messages.MessageKeys;
import gov.va.bip.framework.messages.MessageSeverity;

public class BipValidationRuntimeExceptionTest {


	private static final MessageKey TEST_KEY = MessageKeys.NO_KEY;

	@Test
	public void initializeBipValidationRuntimeExceptionTest() {
		assertNotNull(new BipValidationRuntimeException(TEST_KEY, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST));
		assertNotNull(new BipValidationRuntimeException(TEST_KEY, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST,
				new Exception()));
	}
}
