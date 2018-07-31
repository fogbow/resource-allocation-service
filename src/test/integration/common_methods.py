import requests
import time
from general_configuration_test import GeneralConfigurations

class CommonMethods:

	computes_url= GeneralConfigurations.base_url + GeneralConfigurations.computes_endpoint
	networks_url= GeneralConfigurations.base_url + GeneralConfigurations.networks_endpoint
	volumes_url= GeneralConfigurations.base_url + GeneralConfigurations.volumes_endpoint

	compute_data = {'vCPU': GeneralConfigurations.vCPU, 'memory': GeneralConfigurations.memory, 'disk': GeneralConfigurations.disk, 'providingMember': GeneralConfigurations.local_member, 'imageId': GeneralConfigurations.imageId, 'publicKey': GeneralConfigurations.publicKey}
	network_data = {'address': GeneralConfigurations.address, 'allocation': GeneralConfigurations.allocation}
	volume_data = {}

	@classmethod
	def wait_instance_ready(cls, order_id, order_type):
		state_key = 'state'
		ready_state = 'READY'
		for x in range(GeneralConfigurations.max_tries + 1):
			response = cls.get_order_by_id(order_id, order_type)
			json_response = response.json()
			if json_response[state_key] != ready_state:
				if(x < GeneralConfigurations.max_tries):
					time.sleep(GeneralConfigurations.sleep_time_secs)
					continue
				return False
			break
		return True

	@classmethod
	def post_order(cls, optional_attributes, order_type):
		if order_type == GeneralConfigurations.compute:
			merged_data = cls.compute_data.copy()
			merged_data.update(optional_attributes)
			response = requests.post(cls.computes_url, json= merged_data, headers= GeneralConfigurations.json_header)
			return response
		elif order_type == GeneralConfigurations.network:
			merged_data = cls.network_data.copy()
			merged_data.update(optional_attributes)
			response = requests.post(cls.networks_url, json= merged_data, headers= GeneralConfigurations.json_header)
			return response
		elif order_type == GeneralConfigurations.volume:
			merged_data = cls.volume_data.copy()
			merged_data.update(optional_attributes)
			response = requests.post(cls.volumes_url, json= merged_data, headers= GeneralConfigurations.json_header)
			return response

	@classmethod
	def post_multiple_computes(cls, optional_attributes, amount, order_type):
		orders_id = []
		for i in range(amount):
			response = cls.post_order(optional_attributes, order_type)
			if response.status_code != GeneralConfigurations.created_status:
				print('Test get all computes: Failed on creating compute, trying next test')
				return
			orders_id.append(response.text)
		return orders_id

	@classmethod
	def get_order_by_id(cls, order_id, order_type):
		if order_type == GeneralConfigurations.compute:
			return requests.get(cls.computes_url + order_id)
		elif order_type == GeneralConfigurations.network:
			return requests.get(cls.networks_url + order_id)
		elif order_type == GeneralConfigurations.volume:
			return requests.get(cls.volumes_url + order_id)

	@classmethod
	def delete_order(cls, order_id, order_type):
		if order_type == GeneralConfigurations.compute:
			return requests.delete(cls.computes_url + order_id)
		elif order_type == GeneralConfigurations.network:
			return requests.delete(cls.networks_url + order_id)
		elif order_type == GeneralConfigurations.volume:
			return requests.delete(cls.volumes_url + order_id)
