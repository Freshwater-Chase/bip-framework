package gov.va.ocp.framework.ws.client.remote;

import org.springframework.ws.client.core.WebServiceTemplate;

import gov.va.ocp.framework.transfer.PartnerTransferObjectMarker;

/**
 * Interface for executing remote service calls.
 * The implementation may be real, or may mock the call for testing. Mocking implementations of this interface should extend
 * {@link AbstractRemoteServiceCallMock}.
 *
 * <p>
 * Classes that are either partner implementations or simulator mocks should implement
 * this interface.
 * </p>
 */
@FunctionalInterface
public interface RemoteServiceCall {

	/**
	 * Execution of a real or mocked remote call to the web service identified by the WebServiceTemplate.
	 *
	 * @param webserviceTemplate the template for the web service being called
	 * @param request the request (a subclass of AbstractTransferObject)
	 * @param requestClass the actual Class of the request object
	 * @return PartnerTransferObjectMarker the response from the remote web service (cast it to the desired response type)
	 */
	PartnerTransferObjectMarker callRemoteService(final WebServiceTemplate webserviceTemplate,
			final PartnerTransferObjectMarker request,
			final Class<? extends PartnerTransferObjectMarker> requestClass);

}