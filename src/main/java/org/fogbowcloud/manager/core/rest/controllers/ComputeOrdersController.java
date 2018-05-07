package org.fogbowcloud.manager.core.rest.controllers;

import java.util.List;

import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.rest.services.ComputeOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = "compute")
public class ComputeOrdersController {

	@Autowired
	private ComputeOrdersService computeOrdersService;

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Order> createCompute(@RequestBody ComputeOrder computeOrder,
		@RequestHeader(value = "accessId") String accessId, @RequestHeader(value = "localTokenId") String localTokenId) {
		return computeOrdersService.createCompute(computeOrder, accessId, localTokenId);
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<ComputeOrder>> getAllCompute() {
		return new ResponseEntity<List<ComputeOrder>>(HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<ComputeOrder> getComputeById(@PathVariable Long id) {
		return new ResponseEntity<ComputeOrder>(HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Boolean> deleteCompute(@PathVariable Long id) {
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
}
