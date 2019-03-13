package gov.va.ocp.framework.feign.autoconfigure;

import java.io.IOException;
import java.io.Reader;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpStatus;

import feign.Response;
import feign.codec.ErrorDecoder;
import gov.va.ocp.framework.exception.impl.OcpFeignRuntimeException;
import gov.va.ocp.framework.log.OcpLogger;
import gov.va.ocp.framework.log.OcpLoggerFactory;
import gov.va.ocp.framework.messages.MessageSeverity;

/**
 * The Class FeignCustomErrorDecoder.
 */
public class FeignCustomErrorDecoder implements ErrorDecoder {

	private static final OcpLogger LOGGER = OcpLoggerFactory.getLogger(FeignCustomErrorDecoder.class);

	private final ErrorDecoder defaultErrorDecoder = new Default();

	/*
	 * (non-Javadoc)
	 * 
	 * @see feign.codec.ErrorDecoder#decode(java.lang.String, feign.Response)
	 */
	@Override
	public Exception decode(final String methodKey, final Response response) {
		if ((response.status() >= 400) && (response.status() <= 499)) {

			StringBuilder strBuffer = new StringBuilder();
			try {

				Reader inputReader = response.body().asReader();
				int data = inputReader.read();
				while (data != -1) {
					strBuffer.append((char) data);
					data = inputReader.read();
				}

			} catch (IOException e) {
				LOGGER.debug(
						"Could not read response body, trying alternate methods of error decoding as implemented in decode() method of feign.codec.ErrorDecoder.Default.Default()",
						e);
				return defaultErrorDecoder.decode(methodKey, response);
			}

			try {
				JSONObject messageObjects = new JSONObject(strBuffer.toString());
				JSONArray jsonarray = messageObjects.getJSONArray("messages");
				JSONObject messageObject = jsonarray.getJSONObject(0);
				return new OcpFeignRuntimeException(messageObject.getString("key"), messageObject.getString("text"),
						MessageSeverity.fromValue(messageObject.getString("severity")),
						HttpStatus.resolve(Integer.valueOf(messageObject.getString("status"))));

			} catch (JSONException e) {
				LOGGER.debug(
						"Could not, trying alternate methods of error decoding as implemented in decode() method of feign.codec.ErrorDecoder.Default.Default()",
						e);
				return defaultErrorDecoder.decode(methodKey, response);
			}

		}
		return defaultErrorDecoder.decode(methodKey, response);
	}

}