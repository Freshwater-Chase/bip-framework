package gov.va.bip.framework.rest.exception;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MediaTypeNotSupportedStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.va.bip.framework.exception.BipExceptionExtender;
import gov.va.bip.framework.exception.BipPartnerException;
import gov.va.bip.framework.exception.BipPartnerRuntimeException;
import gov.va.bip.framework.exception.BipRuntimeException;
import gov.va.bip.framework.messages.MessageKey;
import gov.va.bip.framework.messages.MessageKeys;
import gov.va.bip.framework.messages.MessageSeverity;
import gov.va.bip.framework.rest.provider.ProviderResponse;

/**
 * A global exception handler as the last line of defense before sending response to the service consumer.
 * This class converts exceptions to appropriate {@link gov.va.bip.framework.rest.provider.Message} objects and puts them on the
 * response.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BipRestGlobalExceptionHandler {

	/** The Constant LOGGER. */
	private static final Logger logger = LoggerFactory.getLogger(BipRestGlobalExceptionHandler.class);

	/**
	 * Return value if no exception exists to provide a message.
	 * To get default message text, use {@link #deriveMessage(Exception)}.
	 */
	private static final String NO_EXCEPTION_MESSAGE = "Source exception has no message.";

	/**
	 * For java.lang.Exception and all subclasses.
	 * If exception message is empty, gets message of the cause if it exists.
	 *
	 * @param ex the Exception
	 * @return String the message
	 */
	private String deriveMessage(Exception ex, MessageKey key, Object... params) {
		MessageKey derivedKey = deriveKey(key);
		String msg = derivedKey.getMessage(params);
		if (StringUtils.isBlank(msg) || msg.matches("\\{[a-zA-Z0-9]{0,64}\\}")) {
			msg = msg + " :: "
					+ (ex != null && !StringUtils.isBlank(ex.getMessage())
							? ex.getMessage()
							: (ex != null && ex.getCause() != null && !StringUtils.isBlank(ex.getCause().getMessage())
									? ex.getCause().getMessage()
									: NO_EXCEPTION_MESSAGE));
		}
		return msg;
	}

	/**
	 * If key is null, returns the "NO-KEY" key.
	 *
	 * @param key - the initial string intended to represent the key
	 * @return MessageKey - the key, or NO_KEY
	 */
	private MessageKey deriveKey(MessageKey key) {
		return ObjectUtils.defaultIfNull(key, MessageKeys.NO_KEY);
	}

	/**
	 * INFO logs the exception and its details.
	 *
	 * @param ex - the exception
	 * @param key - the key to use for reporting to support/maintenance
	 * @param severity - the MessageSeverity to report for the exception
	 * @param status - the status to report for the exception
	 * @param params - arguments to fill in any params in the MessageKey message (e.g. value for {0})
	 */
	private void log(Exception ex, MessageKey key, MessageSeverity severity, HttpStatus status, Object... params) {
		log(Level.INFO, ex, key, severity, status, params);
	}

	/**
	 * Logs the exception and its details.
	 *
	 * @param level - the Log Level to log at
	 * @param ex - the exception
	 * @param key - the key to use for reporting to support/maintenance
	 * @param severity - the MessageSeverity to report for the exception
	 * @param status - the status to report for the exception
	 * @param params - arguments to fill in any params in the MessageKey message (e.g. value for {0})
	 */
	private void log(Level level, Exception ex, MessageKey key, MessageSeverity severity, HttpStatus status, Object... params) {
		MessageKey derivedKey = deriveKey(key);
		String msg = status + "-" + severity + " "
				+ (ex == null ? "null" : ex.getClass().getName()) + " "
				+ derivedKey + ":" + derivedKey.getMessage(params);
		if (Level.ERROR.equals(level)) {
			logger.error(msg, ex);
		} else if (Level.WARN.equals(level)) {
			logger.warn(msg, ex);
		} else if (Level.INFO.equals(level)) {
			logger.info(msg, ex);
		} else {
			logger.debug(msg, ex);
		}
	}

	/**
	 * A last resort to return a (somewhat) meaningful response to the consumer when there is no source exception.
	 *
	 * @return ResponseEntity the HTTP Response Entity
	 */
	protected ResponseEntity<Object> failSafeHandler() {
		log(Level.ERROR, null, MessageKeys.NO_KEY, MessageSeverity.FATAL, HttpStatus.INTERNAL_SERVER_ERROR);
		ProviderResponse apiError = new ProviderResponse();
		apiError.addMessage(MessageSeverity.FATAL, MessageKeys.NO_KEY.getKey(), MessageKeys.NO_KEY.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Standard exception handler for any exception that implements {@link BipExceptionExtender}.
	 *
	 * @param ex - the exception that implements BipExceptionExtender
	 * @param httpResponseStatus - the status to put on the HTTP Response Entity.
	 * @return ResponseEntity - the HTTP Response Entity
	 */
	protected ResponseEntity<Object> standardHandler(final BipExceptionExtender ex, final HttpStatus httpResponseStatus) {
		if ((ex == null) || (ex.getMessageKey() == null)) {
			return failSafeHandler();
		}
		return standardHandler((Exception) ex, ex.getMessageKey(), ex.getSeverity(), httpResponseStatus);
	}

	/**
	 * Standard exception handler for any Exception.
	 *
	 * @param ex - the Exception
	 * @param key - the key to use for reporting to support/maintenance
	 * @param severity - the MessageSeverity to report for the exception
	 * @param httpResponseStatus - the status to put on the HTTP Response Entity.
	 * @param params - arguments to fill in any params in the MessageKey message (e.g. value for {0})
	 * @return ResponseEntity the HTTP Response Entity
	 */
	protected ResponseEntity<Object> standardHandler(Exception ex, MessageKey key, MessageSeverity severity,
			HttpStatus httpResponseStatus, Object... params) {
		if (ex == null) {
			return failSafeHandler();
		}
		ProviderResponse apiError = new ProviderResponse();

		MessageKey derivedKey = deriveKey(key);
		log(ex, derivedKey, severity, httpResponseStatus, params);
		apiError.addMessage(severity, derivedKey.getKey(), deriveMessage(ex, derivedKey, params), httpResponseStatus);

		return new ResponseEntity<>(apiError, httpResponseStatus);
	}

	// 400

	/**
	 * Handle BipPartnerRuntimeException.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = BipPartnerRuntimeException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleBipPartnerRuntimeException(HttpServletRequest req, BipPartnerRuntimeException ex) {
		return standardHandler(ex, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle BipPartnerException.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = BipPartnerException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleBipPartnerCheckedException(HttpServletRequest req, BipPartnerException ex) {
		return standardHandler(ex, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle illegal argument exception.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = IllegalArgumentException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleIllegalArgumentException(HttpServletRequest req, IllegalArgumentException ex) {
		return standardHandler(ex, MessageKeys.NO_KEY, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle illegal state exceptions - developer attempting to instantiate a class that is for statics.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = IllegalStateException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleIllegalStateException(HttpServletRequest req, IllegalStateException ex) {
		return standardHandler(ex, MessageKeys.BIP_DEV_ILLEGAL_INSTANTIATION, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle {@code @Valid} failures when method argument is not valid.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleMethodArgumentNotValidException(HttpServletRequest req,
			MethodArgumentNotValidException ex) {

		final ProviderResponse apiError = new ProviderResponse();
		if ((ex == null) || (ex.getBindingResult() == null)) {
			return failSafeHandler();
		} else {
			MessageKey key = MessageKeys.BIP_GLOBAL_VALIDATOR_METHOD_ARGUMENT_NOT_VALID;
			for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
				String errorCodes = String.join(", ", error.getCodes());
				Object[] params = new Object[] { "field", errorCodes, error.getDefaultMessage() };
				log(ex, key, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST, params);
				apiError.addMessage(MessageSeverity.ERROR, errorCodes,
						error.getDefaultMessage(), HttpStatus.BAD_REQUEST);
			}
			for (final ObjectError error : ex.getBindingResult().getGlobalErrors()) {
				String errorCodes = String.join(", ", error.getCodes());
				Object[] params = new Object[] { "object", errorCodes, error.getDefaultMessage() };
				log(ex, key, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST, params);
				apiError.addMessage(MessageSeverity.ERROR, errorCodes,
						error.getDefaultMessage(), HttpStatus.BAD_REQUEST);
			}
		}
		return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle HTTP 4xx response in http client.
	 *
	 * @param req the req
	 * @param httpClientErrorException the http client error exception
	 * @return the response entity
	 */
	@ExceptionHandler(value = HttpClientErrorException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleHttpClientErrorException(HttpServletRequest req,
			final HttpClientErrorException httpClientErrorException) {

		ProviderResponse apiError = new ProviderResponse();
		if (httpClientErrorException == null) {
			return failSafeHandler();
		} else {
			MessageKey key = MessageKeys.BIP_GLOBAL_HTTP_CLIENT_ERROR;
			HttpStatus status = httpClientErrorException.getStatusCode();
			String statusReason = httpClientErrorException.getStatusCode().getReasonPhrase();
			Object[] params = new Object[] { statusReason, httpClientErrorException.getMessage() };

			log(httpClientErrorException, key, MessageSeverity.ERROR, status, params);

			byte[] responseBody = httpClientErrorException.getResponseBodyAsByteArray();

			try {
				apiError = new ObjectMapper().readValue(responseBody, ProviderResponse.class);
			} catch (IOException e) {
				log(e, MessageKeys.BIP_GLOBAL_GENERAL_EXCEPTION, MessageSeverity.ERROR, status,
						params);
				apiError.addMessage(MessageSeverity.ERROR, key.getKey(),
						new String(responseBody),
						httpClientErrorException.getStatusCode());
			}
		}

		return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle method argument type mismatch.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleMethodArgumentTypeMismatch(HttpServletRequest req,
			final MethodArgumentTypeMismatchException ex) {

		MessageKey key = MessageKeys.BIP_GLOBAL_REST_API_TYPE_MISMATCH;
		Object[] params = new Object[] { ex.getName(), ex.getRequiredType().getName() };

		log(Level.INFO, ex, key, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST, params);

		final ProviderResponse apiError = new ProviderResponse();
		apiError.addMessage(MessageSeverity.ERROR, key.getKey(),
				key.getMessage(params), HttpStatus.BAD_REQUEST);
		return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle constraint violation.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = ConstraintViolationException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleConstraintViolation(HttpServletRequest req, final ConstraintViolationException ex) {

		final ProviderResponse apiError = new ProviderResponse();
		if ((ex == null) || (ex.getConstraintViolations() == null)) {
			return failSafeHandler();
		} else {
			MessageKey key = MessageKeys.BIP_GLBOAL_VALIDATOR_CONSTRAINT_VIOLATION;
			for (final ConstraintViolation<?> violation : ex.getConstraintViolations()) {
				Object[] params =
						new Object[] { violation.getRootBeanClass().getName(), violation.getPropertyPath(), violation.getMessage() };
				log(ex, key, MessageSeverity.ERROR, HttpStatus.BAD_REQUEST, params);
				apiError.addMessage(MessageSeverity.ERROR, key.getKey(),
						key.getMessage(params), HttpStatus.BAD_REQUEST);
			}
		}
		return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handle http message not readable exception.
	 *
	 * @param req the req
	 * @param httpMessageNotReadableException the http message not readable exception
	 * @return the response entity
	 */
	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public final ResponseEntity<Object> handleHttpMessageNotReadableException(HttpServletRequest req,
			final HttpMessageNotReadableException httpMessageNotReadableException) {
		return standardHandler(httpMessageNotReadableException, MessageKeys.NO_KEY, MessageSeverity.ERROR,
				HttpStatus.BAD_REQUEST);
	}

	// 404

	/**
	 * Handle no handler found exception.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = NoHandlerFoundException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public final ResponseEntity<Object> handleNoHandlerFoundException(HttpServletRequest req, final NoHandlerFoundException ex) {
		return standardHandler(ex, MessageKeys.NO_KEY, MessageSeverity.ERROR, HttpStatus.NOT_FOUND);
	}

	// 405

	/**
	 * Handle http request method not supported.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
	public final ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpServletRequest req,
			final HttpRequestMethodNotSupportedException ex) {
		return standardHandler(ex, MessageKeys.NO_KEY, MessageSeverity.ERROR, HttpStatus.METHOD_NOT_ALLOWED);
	}

	// 415

	/**
	 * Handle http media type not supported.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = MediaTypeNotSupportedStatusException.class)
	@ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public final ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpServletRequest req,
			final HttpMediaTypeNotSupportedException ex) {
		return standardHandler(ex, MessageKeys.NO_KEY, MessageSeverity.ERROR, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	// Handle all

	/**
	 * Handle bip runtime exception.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = BipRuntimeException.class)
	public final ResponseEntity<Object> handleBipRuntimeException(HttpServletRequest req, BipRuntimeException ex) {
		return standardHandler(ex, ex.getStatus());
	}

	/**
	 * Handle all.
	 *
	 * @param req the req
	 * @param ex the ex
	 * @return the response entity
	 */
	@ExceptionHandler(value = Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public final ResponseEntity<Object> handleAll(HttpServletRequest req, final Exception ex) {
		return standardHandler(ex, MessageKeys.NO_KEY, MessageSeverity.FATAL, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}