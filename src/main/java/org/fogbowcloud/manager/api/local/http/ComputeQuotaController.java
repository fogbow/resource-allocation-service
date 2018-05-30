package org.fogbowcloud.manager.api.local.http;

import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value = ComputeQuotaController.QUOTA_ENDPOINT)
public class ComputeQuotaController {

	public static final String QUOTA_ENDPOINT = "quota";

	private final String FEDERATION_TOKEN_VALUE_HEADER_KEY = "federationTokenValue";
	private final Logger LOGGER = LoggerFactory.getLogger(ComputeQuotaController.class);

	@RequestMapping(value = "/shared/{id}", method = RequestMethod.GET)
	public ResponseEntity<ComputeQuota> getSharedQuota(@PathVariable String memberId,
			@RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
			throws UnauthenticatedException {

		LOGGER.info("Shared quota information request received.");

		ComputeQuota quotaInstance = ApplicationFacade.getInstance().getSharedQuota(memberId, federationTokenValue);
		return new ResponseEntity<>(quotaInstance, HttpStatus.OK);
	}

	@RequestMapping(value = "/used/{id}", method = RequestMethod.GET)
	public ResponseEntity<ComputeQuota> getUsedQuota(@PathVariable String memberId,
			@RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
			throws UnauthenticatedException {

		LOGGER.info("Used quota information request received.");

		ComputeQuota quotaInstance = ApplicationFacade.getInstance().getUsedQuota(memberId, federationTokenValue);
		return new ResponseEntity<>(quotaInstance, HttpStatus.OK);
	}

	@RequestMapping(value = "/my/{id}", method = RequestMethod.GET)
	public ResponseEntity<ComputeQuota> getInUseByMeQuota(@PathVariable String memberId,
			@RequestHeader(value = FEDERATION_TOKEN_VALUE_HEADER_KEY) String federationTokenValue)
			throws UnauthenticatedException {

		LOGGER.info("In use quota information request received.");

		ComputeQuota quotaInstance = ApplicationFacade.getInstance().getInUseQuota(memberId, federationTokenValue);
		return new ResponseEntity<>(quotaInstance, HttpStatus.OK);
	}
}
