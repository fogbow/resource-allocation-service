package org.fogbowcloud.manager.core.rest;

import java.util.List;

import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value = "compute")
public class ComputeOrdersController {
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<String> createCompute(@RequestBody ComputeOrder ComputeOrder) {
		return new ResponseEntity<String>(HttpStatus.OK);
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
