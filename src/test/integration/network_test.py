import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class NetworkTests:

  test_number = 0

  @classmethod
  def test_networks(cls):
    data_for_local = {}
    print('-Test %d: post local network' % cls.which_test_case())
    cls.test_post_networks(data_for_local)
    print('-Test %d: get local network by id' % cls.which_test_case())
    cls.test_get_by_id_network(data_for_local)
    print('-Test %d: get all local networks' % cls.which_test_case())
    cls.test_get_all_networks(data_for_local)
    print('-Test %d: delete local network' % cls.which_test_case())
    cls.test_delete_network(data_for_local)
    if GeneralConfigurations.remote_member:
      data_for_remote = {GeneralConfigurations.provider: GeneralConfigurations.remote_member}
      print('-Test %d: post remote network' % cls.which_test_case())
      cls.test_post_networks(data_for_remote)
      print('-Test %d: get remote network by id' % cls.which_test_case())
      cls.test_get_by_id_network(data_for_remote)
      print('-Test %d: get all remote networks' % cls.which_test_case())
      cls.test_get_all_networks(data_for_remote)
      print('-Test %d: delete remote network' % cls.which_test_case())
      cls.test_delete_network(data_for_remote)

  @classmethod
  def which_test_case(cls):
    cls.test_number += 1
    return cls.test_number

  #Post tests
  @classmethod
  def test_post_networks(cls, data):
    extra_data = {}
    extra_data.update(data)
    order_id = CommonMethods.post_network(extra_data)
    if not order_id:
      print('  Failed. Could not create network')
      return False
    if CommonMethods.wait_instance_ready(order_id, GeneralConfigurations.type_network):
      print('  Ok. Removing network')
    else:
      print('  Failed. Network was not ready')
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)

  # Get tests
  @classmethod
  def test_get_by_id_network(cls, data):
    extra_data = {}
    extra_data.update(data)
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_network)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('  Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    order_id = CommonMethods.post_network(extra_data)
    if not order_id:
      print('  Failed when creating network, trying next test')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    response_get = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_network)
    if response_get.status_code == GeneralConfigurations.ok_status:
      print('  Ok. Removing network')
    else:
      print('  Failed. Expecting %d status, but got: %d. Removing network' % (GeneralConfigurations.ok_status, response_get.status_code))
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)
    time.sleep(10)

  @classmethod
  def test_get_all_networks(cls, data):
    extra_data = {}
    extra_data.update(data)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_network)
    time.sleep(10)
    if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
      print('  Failed. There was a network created already. Received http status %d and message: %s' % (response_get.status_code, response_get.text))
      return
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_networks, GeneralConfigurations.type_network)
    if not orders_id:
      print('  Failed. Could not create networks')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_network)
    test_ok =  not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_networks)
    if test_ok:
      print('  Ok. Removing networks')
    else:
      print('  Failed. Removing networks')
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_network)
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(40)
    else:
      time.sleep(10)

  # Delete tests
  @classmethod
  def test_delete_network(cls, data):
    extra_data = {}
    extra_data.update(data)
    order_id = CommonMethods.post_network(extra_data)
    if not order_id:
      print('  Failed. Could not crete network')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_network)
    if (get_response.status_code == GeneralConfigurations.not_found_status):
      print('  Failed. Got http status: %d' % (get_response.status_code))
      CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)
      return
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_network)
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_network)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print('  Failed. Got http status %d and was expected: %d' % (get_response.status_code, GeneralConfigurations.not_found_status))
      return
    print('  Ok. Network removed')
    time.sleep(10)