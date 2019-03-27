package gov.va.ocp.framework.ws.client.interceptor;

import org.springframework.ws.client.core.WebServiceTemplate;

import gov.va.ocp.framework.audit.AuditEventData;
import gov.va.ocp.framework.audit.AuditEvents;

/**
 * Configurations of audit metadata that are available for use with the {@link AuditWsInterceptor}.
 *
 * @author aburkholder
 */
public enum AuditWsInterceptorConfig {

	/** Configuration intended for auditing objects <b><i>before</i></b> other ClientInterceptors run */
	BEFORE("Raw XML"),
	/** Configuration intended for auditing objects <b><i>after</i></b> other ClientInterceptors run */
	AFTER("Wire Log");

	/** The title string used in the audit message prefix */
	private String title;

	/** The class reported as being under audit */
	private static final Class<WebServiceTemplate> AUDITED = WebServiceTemplate.class;
	/** System new-line character */
	private static final String NEW_LINE = System.getProperty("line.separator");
	/** Arrow used in message prefixes */
	private static final String ARROW = " -> ";

	/**
	 * Instantiate the webservice audit interceptor.
	 *
	 * @param title - the String to be used in the audit message prefix
	 */
	AuditWsInterceptorConfig(String title) {
		this.title = title;
	}

	/**
	 * What class is deemed to be under audit.
	 *
	 * @return Class - the class deemed to be under audit
	 */
	Class<?> audited() {
		return AUDITED;
	}

	/**
	 * The simple name of the class that is deemed to be under audit.
	 *
	 * @return String - the simple name of the class that is deemed to be under audit
	 */
	String auditedName() {
		return AUDITED.getSimpleName();
	}

	/**
	 * Inner abstract class to declare scoped (non-public) methods for audit logging metadata.
	 *
	 * @author aburkholder
	 */
	abstract class AuditWsMetadata {
		/** Get a new AuditEventData instance for the data object being audited */
		abstract AuditEventData eventData();

		/** Get the AuditEvents enum for the audit event */
		abstract AuditEvents event();

		/** Get the activity identifier string for the audit event */
		abstract String activity();

		/** Get the message prefix string for the data object being audited */
		abstract String messagePrefix();
	}

	/**
	 * Class that declares the auditing metadata for webservice <b><i>request</i></b> objects.
	 *
	 * @see AuditWsMetadata
	 * @author aburkholder
	 */
	class Request extends AuditWsMetadata {
		/** the ACTIVITY identifier string for the audit event */
		private static final String ACTIVITY = "webserviceRequest";

		@Override
		AuditEventData eventData() {
			return new AuditEventData(AuditEvents.PARTNER_SOAP_REQUEST, ACTIVITY, AUDITED.getName());
		}

		@Override
		AuditEvents event() {
			return AuditEvents.PARTNER_SOAP_REQUEST;
		}

		@Override
		String activity() {
			return ACTIVITY;
		}

		@Override
		String messagePrefix() {
			return ACTIVITY + ARROW + title + " : " + NEW_LINE;
		}
	}

	/**
	 * The auditing metadata for webservice <b><i>request</i></b> objects.
	 *
	 * @return Request - the metadata for auditing webservice request objects
	 */
	Request request() {
		return new Request();
	}

	/**
	 * Class that declares the auditing metadata for webservice <b><i>response</i></b> objects.
	 *
	 * @see AuditWsMetadata
	 * @author aburkholder
	 */
	class Response extends AuditWsMetadata {
		/** the ACTIVITY identifier string for the audit event */
		static final String ACTIVITY = "webserviceResponse";

		@Override
		AuditEventData eventData() {
			return new AuditEventData(AuditEvents.PARTNER_SOAP_RESPONSE, ACTIVITY, AUDITED.getName());
		}

		@Override
		AuditEvents event() {
			return AuditEvents.PARTNER_SOAP_RESPONSE;
		}

		@Override
		String activity() {
			return ACTIVITY;
		}

		@Override
		String messagePrefix() {
			return ACTIVITY + ARROW + title + " : " + NEW_LINE;
		}
	}

	/**
	 * The auditing metadata for webservice <b><i>response</i></b> objects.
	 *
	 * @return Response - the metadata for auditing webservice response objects
	 */
	Response response() {
		return new Response();
	}

	/**
	 * Class that declares the auditing metadata for webservice <b><i>fault</i></b> objects.
	 *
	 * @see AuditWsMetadata
	 * @author aburkholder
	 */
	class Fault extends Response {

		/** the activity identifier string for the audit event */
		static final String ACTIVITY = "webserviceResponse_SOAP-FAULT";
	}

	/**
	 * The auditing metadata for webservice <b><i>response</i></b> objects.
	 *
	 * @return Response - the metadata for auditing webservice response objects
	 */
	Fault fault() {
		return new Fault();
	}
}