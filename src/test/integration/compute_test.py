import requests
import time
from general_configuration_test import GeneralConfigurations

class ComputeTests:

	computes_url = GeneralConfigurations.base_url + GeneralConfigurations.computes_endpoint

	def __init__(self):
		self.test_computes()

	def test_computes(self):
		self.test_post_local_compute()
		self.test_delete_local_compute()
		self.test_get_all_local_compute()
		self.test_get_by_id_local_compute()
		self.test_local_quota()
		self.test_local_allocation()

	# Post functions
	def test_post_local_compute(self):
		data = {}
		response = self.post_compute(data)
		if response.status_code != GeneralConfigurations.created_status:
			print('Test post local compute: Failed, trying next test')
			return False
		order_id = response.text
		if self.wait_instance_ready(order_id):
			print('Test post local compute: Ok. Removing compute')
		else:
			print('Test post local compute: Failed. Removing compute')
		self.delete_compute(order_id)

	def post_compute(self, optional_attributes):
		data = {'vCPU': GeneralConfigurations.vCPU, 'memory': GeneralConfigurations.memory, 'disk': GeneralConfigurations.disk, 'providingMember': GeneralConfigurations.local_member, 'imageId': GeneralConfigurations.imageId, 'publicKey': GeneralConfigurations.publicKey}
		merged_data = data.copy()
		merged_data.update(optional_attributes)
		response = requests.post(self.__class__.computes_url, json= merged_data, headers= GeneralConfigurations.json_header)
		return response

	def post_multiple_computes(self, optional_attributes, amount):
		orders_id = []
		for i in range(amount):
			response = self.post_compute(optional_attributes)
			if response.status_code != GeneralConfigurations.created_status:
				print('Test get all computes: Failed on creating compute, trying next test')
				return
			orders_id.append(response.text)
		return orders_id

	# Quota functions
	def test_local_quota(self):
		response_get_quota = self.get_quota(GeneralConfigurations.local_member)
		if response_get_quota.status_code != 200:
			print('Test get local quota: Failed, trying next test')
			return
		if response_get_quota.json() != '':
			print('Test get local quota: Ok, trying next test')
		else:
			print('Test get local quota: Failed, trying next test')

	def get_quota(self, member):
		response = requests.get(self.__class__.computes_url + GeneralConfigurations.quota_endpoint + member, headers= GeneralConfigurations.json_header)
		return response

	def get_available_instances(self, member):
		response_get_quota = self.get_quota(GeneralConfigurations.local_member)
		available_quota = response_get_quota.json()[GeneralConfigurations.available_quota]
		return available_quota[GeneralConfigurations.instances_quota]

	def wait_instances_available(self, size):
		for x in range(GeneralConfigurations.max_tries + 1):
			instances_available = self.get_available_instances(size)
			if instances_available < size:
				print('No instances available, waiting for resources')
				if(x < GeneralConfigurations.max_tries):
					time.sleep(GeneralConfigurations.sleep_time_secs)
					continue
				print('No instances available')
				return False
			break
		print('Instances are now available, proceeding')
		return True

	# Allocation
	def test_local_allocation(self):
		if not self.wait_instances_available(GeneralConfigurations.max_computes):
			print('Test get all computes: Failed. There is not %d instances available.')
			return
		response_get_allocation = self.get_allocation(GeneralConfigurations.local_member)
		if response_get_allocation.status_code != 200:
			print('Test get local allocation: Failed, trying next test')
			return
		if not self.empty_allocation(response_get_allocation.json()):
			print(response_get_allocation.json())
			print('Test get local allocation: Failed, allocation already in use, trying next test')
			return
		data = {}
		orders_id = self.post_multiple_computes(data, GeneralConfigurations.max_computes)
		if not orders_id:
			print('Test get all computes: Failed. Could not create computes')
			return
		for order in orders_id:
			self.wait_instance_ready(order)
		response_get_allocation = self.get_allocation(GeneralConfigurations.local_member)
		allocation = response_get_allocation.json()
		if self.empty_allocation(allocation):
			print('Test get local allocation: Failed, allocation already in use, trying next test')
			return
		if allocation['instances'] == GeneralConfigurations.max_computes:
			print('Test get allocation: Ok. Removing compute')
		else:
			print('Test get allocation: Failed. Removing compute')
		self.delete_multiple_computes(orders_id)


	def get_allocation(self, member):
		response = requests.get(self.__class__.computes_url + GeneralConfigurations.allocation_endpoint + member, headers= GeneralConfigurations.json_header)
		return response

	def empty_allocation(self, allocation):
		if allocation['vCPU'] == 0 and allocation['ram'] == 0 and allocation['instances'] == 0:
			return True
		return False

	# Get functions
	def test_get_by_id_local_compute(self):
		data = {}
		fake_id = 'fake-id'
		response_get = self.get_compute_by_id(fake_id)
		if response_get.status_code != GeneralConfigurations.not_found_status:
			print('Test get compute by id: Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
			return
		response_post = self.post_compute(data)
		order_id = response_post.text
		if response_post.status_code != GeneralConfigurations.created_status:
			print('Test get by id: Failed on creating compute, trying next test')
			self.delete_compute(order_id)
			return
		response_get = self.get_compute_by_id(order_id)
		test_ok = False
		if response_get.status_code == GeneralConfigurations.ok_status:
			test_ok = True
		if test_ok:
			print('Test get compute by id: Ok. Removing compute')
		else:
			print('Test get compute by id: Failed. Expecting %d status, but got: %d. Removing compute' % (GeneralConfigurations.ok_status, response_get.status_code))
		self.delete_compute(order_id)


	def test_get_all_local_compute(self):
		if not self.wait_instances_available(GeneralConfigurations.max_computes):
			print('Test get all computes: Failed. There is not %d instances available.')
			return
		response_get = self.get_all_status_computes()
		if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
			print('Test get all computes: Failed')
			return
		data = {}
		orders_id = self.post_multiple_computes(data, GeneralConfigurations.max_computes)
		if not orders_id:
			print('Test get all computes: Failed. Could not create computes')
			return
		response_get = self.get_all_status_computes()
		test_ok = False
		if not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_computes):
			test_ok = True
		if test_ok:
			print('Test get all local compute: Ok. Removing computes')
		else:
			print('Test get all computes: Failed. Removing computes')
		self.delete_multiple_computes(orders_id)

	def get_compute_by_id(self, order_id):
		response = requests.get(self.__class__.computes_url + order_id)
		return response

	def get_all_status_computes(self):
		response = requests.get(self.__class__.computes_url + GeneralConfigurations.status_endpoint)
		return response

	def wait_instance_ready(self, order_id):
		state_key = 'state'
		ready_state = 'READY'
		for x in range(GeneralConfigurations.max_tries + 1):
			response = self.get_compute_by_id(order_id)
			json_response = response.json()
			if json_response[state_key] != ready_state:
				if(x < GeneralConfigurations.max_tries):
					time.sleep(GeneralConfigurations.sleep_time_secs)
					continue
				return False
			break
		return True

	# Delete functions
	def test_delete_local_compute(self):
		data = {}
		response = self.post_compute(data)
		if response.status_code != GeneralConfigurations.created_status:
			print('Test Failed. Trying next test')
			return
		order_id = response.text
		get_response = self.get_compute_by_id(order_id)
		if (get_response.status_code == GeneralConfigurations.not_found_status):
			print('Test Failed. Trying next test')
			return
		self.delete_compute(order_id)
		get_response = self.get_compute_by_id(order_id)
		if (get_response.status_code != GeneralConfigurations.not_found_status):
			print('Test delete local compute: Failed.')
			return
		print('Test delete local compute: Ok. Compute removed')

	def delete_compute(self, order_id):
		response = requests.delete(self.__class__.computes_url + order_id)
		return response

	def delete_multiple_computes(self, orders_id):
		for id in orders_id:
			self.delete_compute(id)