import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class ComputeTests:

  test_number = 0

  @classmethod
  def test_computes(cls):
    data_for_local = {}
    print('-Test %d: post local compute' % cls.which_test_case())
    cls.test_post_compute(data_for_local)
    cls.test_post_local_compute_with_private_network()
    cls.test_delete_local_compute()
    cls.test_get_all_local_compute()
    cls.test_get_by_id_local_compute()
    cls.test_local_quota()
    cls.test_local_allocation()
    if GeneralConfigurations.remote_member:
      print('-Test %d: post remote compute' % cls.which_test_case())
      data_for_remote = {GeneralConfigurations.providingMember: GeneralConfigurations.remote_member}
      cls.test_post_compute(data_for_remote)

  @classmethod
  def which_test_case(cls):
    cls.test_number += 1
    return cls.test_number

  # Post tests
  @classmethod
  def test_post_compute(cls, data):
    if not CommonMethods.wait_compute_available(1):
      print('Failed. There is not %d instance(s) available.' % 1)
      return
    extra_data = {}
    extra_data.update(data)
    order_id = CommonMethods.post_compute(extra_data)
    if not order_id:
      print('  Failed, trying next test')
      return 
    if cls.wait_instance_ready(order_id, GeneralConfigurations.type_compute):
      print('  Ok. Removing compute')
    else:
      print('  Failed. Removing compute')
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)

  @classmethod
  def test_post_local_compute_with_private_network(cls):
    extra_data_network = {}
    networks = []
    network_id = CommonMethods.post_network(extra_data_network)
    networks.append(network_id)
    if not network_id:
      CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)
      print('Test post compute attached to a private network: Failed, could not create new network')
      return 
    if not cls.wait_instance_ready(network_id, GeneralConfigurations.type_network):
      print('Test post compute attached to a private network: Failed. Removing network')
      CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)
    extra_data_compute = {GeneralConfigurations.networksId_key: networks}
    compute_id = CommonMethods.post_compute(extra_data_compute)
    if not compute_id:
      CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)
      print('Test post compute attached to a private network: Failed, could not create new compute')
      return 
    #for now, we do not check any get, because fogbow-core doesn't provide the extra network interface for users
    if cls.wait_instance_ready(compute_id, GeneralConfigurations.type_compute):
      print('Test post compute attached to a private network: Ok. Removing compute')
    else:
      print('Test post compute attached to a private network: Failed. Removing compute')
    response_get = CommonMethods.get_order_by_id(compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)

  # Quota tests
  @classmethod
  def test_local_quota(cls):
    response_get_quota = CommonMethods.get_quota(GeneralConfigurations.local_member)
    if response_get_quota.status_code != 200:
      print('Test get local quota: Failed, trying next test')
      return
    if response_get_quota.json() != '':
      print('Test get local quota: Ok, trying next test')
    else:
      print('Test get local quota: Failed, trying next test')

  # Allocation tests
  @classmethod
  def test_local_allocation(cls):
    if not CommonMethods.wait_compute_available(GeneralConfigurations.max_computes):
      print('Test get all computes: Failed. There is not %d instances available.' % GeneralConfigurations.max_computes)
      return
    response_get_allocation = cls.get_allocation(GeneralConfigurations.local_member)
    if response_get_allocation.status_code != 200:
      print('Test get local allocation: Failed, trying next test')
      return
    if not cls.empty_allocation(response_get_allocation.json()):
      print(response_get_allocation.json())
      print('Test get local allocation: Failed, allocation already in use, trying next test')
      return
    extra_data = {}
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_computes, GeneralConfigurations.type_compute)
    if not orders_id:
      print('Test get all computes: Failed. Could not create computes')
      return
    for order in orders_id:
      cls.wait_instance_ready(order, GeneralConfigurations.type_compute)
    response_get_allocation = cls.get_allocation(GeneralConfigurations.local_member)
    allocation = response_get_allocation.json()
    if cls.empty_allocation(allocation):
      CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_compute)
      print('Test get local allocation: Failed, allocation already in use, trying next test')
      return
    if allocation['instances'] == GeneralConfigurations.max_computes:
      print('Test get allocation: Ok. Removing compute')
    else:
      print('Test get allocation: Failed. Removing compute')
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_compute)


  @classmethod
  def get_allocation(cls, member):
    response = requests.get(CommonMethods.url_computes + GeneralConfigurations.allocation_endpoint + member, headers= GeneralConfigurations.json_header)
    return response

  @classmethod
  def empty_allocation(cls, allocation):
    if allocation['vCPU'] == 0 and allocation['ram'] == 0 and allocation['instances'] == 0:
      return True
    return False

  # Get tests
  @classmethod
  def test_get_by_id_local_compute(cls):
    extra_data = {}
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_compute)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('Test get compute by id: Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    order_id = CommonMethods.post_compute(extra_data)
    if not order_id:
      print('Test get by id: Failed on creating compute, trying next test')
      CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
      #time to wait order to be deleted
      time.sleep(20)
      return
    response_get = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
    if response_get.status_code == GeneralConfigurations.ok_status:
      print('Test get compute by id: Ok. Removing compute')
    else:
      print('Test get compute by id: Failed. Expecting %d status, but got: %d. Removing compute' % (GeneralConfigurations.ok_status, response_get.status_code))
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
    #time to wait order to be deleted
    time.sleep(20)


  @classmethod
  def test_get_all_local_compute(cls):
    if not CommonMethods.wait_compute_available(GeneralConfigurations.max_computes):
      print('Test get all computes: Failed. There is not %d instances available.' % GeneralConfigurations.max_computes)
      return
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_compute)
    if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
      print('Test get all computes: Failed')
      return
    extra_data = {}
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_computes, GeneralConfigurations.type_compute)
    if not orders_id:
      print('Test get all computes: Failed. Could not create computes')
      return
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_compute)
    test_ok = not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_computes)
    if test_ok:
      print('Test get all local compute: Ok. Removing computes')
    else:
      print('Test get all computes: Failed. Removing computes')
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_compute)

  @classmethod
  def wait_instance_ready(cls, order_id, order_type):
    state_key = 'state'
    ready_state = 'READY'
    for x in range(GeneralConfigurations.max_tries + 1):
      response = CommonMethods.get_order_by_id(order_id, order_type)
      if response.status_code != GeneralConfigurations.ok_status:
        if(x < GeneralConfigurations.max_tries):
          time.sleep(GeneralConfigurations.sleep_time_secs)
          continue
        return False
      json_response = response.json()
      if json_response[state_key] != ready_state:
        if(x < GeneralConfigurations.max_tries):
          time.sleep(GeneralConfigurations.sleep_time_secs)
          continue
        return False
      break
    return True

  # Delete tests
  @classmethod
  def test_delete_local_compute(cls):
    extra_data = {}
    order_id = CommonMethods.post_compute(extra_data)
    if not order_id:
      print('Test Failed. Trying next test')
      return
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
    if (get_response.status_code == GeneralConfigurations.not_found_status):
      CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
      #time to wait order to be deleted
      time.sleep(20)
      print('Test Failed. Trying next test')
      return
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print('Test delete local compute: Failed.')
      return
    print('Test delete local compute: Ok. Compute removed')
    #time to wait order to be deleted
    time.sleep(20)