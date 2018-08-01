import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class NetworkTests:

  @classmethod
  def test_networks(cls):
    cls.test_post_local_networks()
    cls.test_get_by_id_local_network()
    cls.test_get_all_local_networks()
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

  # Get tests
  @classmethod
  def test_get_by_id_local_network(cls):
    extra_data = {}
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_network)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('Test get network by id: Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    response_post = CommonMethods.post_order(extra_data, GeneralConfigurations.type_network)
    order_id = response_post.text
    if response_post.status_code != GeneralConfigurations.created_status:
      print('Test get by id: Failed on creating network, trying next test')
      CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)
      return
    response_get = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_network)
    test_ok = False
    if response_get.status_code == GeneralConfigurations.ok_status:
      test_ok = True
    if test_ok:
      print('Test get network by id: Ok. Removing network')
    else:
      print('Test get network by id: Failed. Expecting %d status, but got: %d. Removing network' % (GeneralConfigurations.ok_status, response_get.status_code))
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)

  @classmethod
  def test_get_all_local_networks(cls):
    time.sleep(60)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_network)
    if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
      print('Test get all networks: Failed')
      return
    extra_data = {}
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_networks, GeneralConfigurations.type_network)
    if not orders_id:
      print('Test get all networks: Failed. Could not create networks')
      return
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_network)
    test_ok = False
    if not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_networks):
      test_ok = True
    if test_ok:
      print('Test get all local networks: Ok. Removing networks')
    else:
      print('Test get all networks: Failed. Removing networks')
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_network)

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