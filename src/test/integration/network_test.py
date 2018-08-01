import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class NetworkTests:

	networks_url = GeneralConfigurations.base_url + GeneralConfigurations.networks_endpoint

	@classmethod
	def test_networks(cls):
		cls.test_post_local_networks()
		cls.test_delete_local_network()

	#Post tests
	@classmethod
	def test_post_local_networks(cls):
		extra_data = {}
		response = CommonMethods.post_order(extra_data, GeneralConfigurations.type_network)
		if response.status_code != GeneralConfigurations.created_status:
			print('Test post local network: Failed, trying next test')
			return False
		order_id = response.text
		if CommonMethods.wait_instance_ready(order_id, GeneralConfigurations.type_network):
			print('Test post local network: Ok. Removing network')
		else:
			print('Test post local network: Failed. Removing network')
		CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)

	# Delete tests
	@classmethod
	def test_delete_local_network(cls):
		extra_data = {}
		response = CommonMethods.post_order(extra_data, GeneralConfigurations.type_network)
		if response.status_code != GeneralConfigurations.created_status:
			print('Test Failed. Trying next test')
			return
		order_id = response.text
		get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_network)
		if (get_response.status_code == GeneralConfigurations.not_found_status):
			print('Test Failed. Trying next test')
			return
		CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)
		get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_network)
		if (get_response.status_code != GeneralConfigurations.not_found_status):
			print('Test delete local network: Failed.')
			return
		print('Test delete local network: Ok. Network removed')