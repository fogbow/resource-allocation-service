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
    print('-Test %d: post local compute attached to a local private network' % cls.which_test_case())
    cls.test_post_compute_with_private_network(data_for_local)
    print('-Test %d: delete local compute' % cls.which_test_case())
    cls.test_delete_compute(data_for_local)
    print('-Test %d: get all local computes' % cls.which_test_case())
    cls.test_get_all_compute(data_for_local)
    print('-Test %d: get by id local compute' % cls.which_test_case())
    cls.test_get_by_id_compute(data_for_local)
    print('-Test %d: get local quota' % cls.which_test_case())
    cls.test_quota(data_for_local)
    print('-Test %d: get local allocationMode:' % cls.which_test_case())
    cls.test_allocation(data_for_local)
    if GeneralConfigurations.remote_member:
      data_for_remote = {GeneralConfigurations.provider: GeneralConfigurations.remote_member}
      print('-Test %d: post remote compute' % cls.which_test_case())
      cls.test_post_compute(data_for_remote)
      print('-Test %d: post remote compute attached to a remote private network' % cls.which_test_case())
      cls.test_post_compute_with_private_network(data_for_remote)
      print('-Test %d: delete remote compute' % cls.which_test_case())
      cls.test_delete_compute(data_for_remote)
      print('-Test %d: get all remote computes' % cls.which_test_case())
      cls.test_get_all_compute(data_for_remote)
      print('-Test %d: get by id remote compute' % cls.which_test_case())
      cls.test_get_by_id_compute(data_for_remote)
      print('-Test %d: get remote quota' % cls.which_test_case())
      cls.test_quota(data_for_remote)
      print('-Test %d: get remote allocationMode:' % cls.which_test_case())
      cls.test_allocation(data_for_remote)

  @classmethod
  def which_test_case(cls):
    cls.test_number += 1
    return cls.test_number

  # Post tests
  @classmethod
  def test_post_compute(cls, data):
    extra_data = {}
    extra_data.update(data)
    member = extra_data[GeneralConfigurations.provider] if GeneralConfigurations.provider in extra_data else GeneralConfigurations.local_member
    if not CommonMethods.wait_compute_available(1, member):
      print('  Failed. There is not %d instance(s) available.' % 1)
      return
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
  def test_post_compute_with_private_network(cls, data):
    extra_data_network = {}
    extra_data_network.update(data)
    networks = []
    network_id = CommonMethods.post_network(extra_data_network)
    networks.append(network_id)
    if not network_id:
      CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)
      print('  Failed, could not create new network')
      return 
    if not cls.wait_instance_ready(network_id, GeneralConfigurations.type_network):
      print('  Failed. Network did not transitioned to \'ready\' state')
      CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)
      return
    extra_data_compute = {GeneralConfigurations.networksId_key: networks}
    extra_data_compute.update(data)
    compute_id = CommonMethods.post_compute(extra_data_compute)
    if not compute_id:
      CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)
      print('  Failed, could not create new compute')
      return 
    #for now, we do not check any get, because fogbow-core doesn't provide the extra network interface for users
    if cls.wait_instance_ready(compute_id, GeneralConfigurations.type_compute):
      print('  Ok. Removing compute and network')
    else:
      print('  Failed. Removing compute and network')
    response_get = CommonMethods.get_order_by_id(compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(network_id, GeneralConfigurations.type_network)

  # Quota tests
  @classmethod
  def test_quota(cls, data):
    extra_data = {}
    extra_data.update(data)
    response_get_quota = None
    member = extra_data[GeneralConfigurations.provider] if GeneralConfigurations.provider in extra_data else GeneralConfigurations.local_member
    if GeneralConfigurations.provider in data:
      #if remote
      response_get_quota = CommonMethods.get_quota(data[GeneralConfigurations.provider])
    else:
      response_get_quota = CommonMethods.get_quota(member)
    if response_get_quota.status_code != GeneralConfigurations.ok_status:
      print('  Failed. Got HTTP status: %d and message: %s' % (response_get_quota.status_code, response_get_quota.text))
      return
    #review this test
    if response_get_quota.json() != '':
      print('  Ok')
    else:
      print('  Failed, trying next test')

  # Allocation tests
  @classmethod
  def test_allocation(cls, data):
    extra_data = {}
    extra_data.update(data)
    member = extra_data[GeneralConfigurations.provider] if GeneralConfigurations.provider in extra_data else GeneralConfigurations.local_member
    if not CommonMethods.wait_compute_available(GeneralConfigurations.max_computes, member):
      print('  Failed. There is not %d instances available.' % GeneralConfigurations.max_computes)
      return
    response_get_allocation = cls.get_allocation(member)
    if response_get_allocation.status_code != 200:
      print('  Failed, trying next test')
      return
    if not cls.empty_allocation(response_get_allocation.json()):
      print(response_get_allocation.json())
      print('  Failed, allocationMode already in use, trying next test')
      return
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_computes, GeneralConfigurations.type_compute)
    if not orders_id:
      print('  Failed. Could not create computes')
      return
    for order in orders_id:
      cls.wait_instance_ready(order, GeneralConfigurations.type_compute)
    response_get_allocation = cls.get_allocation(member)
    allocationMode = response_get_allocation.json()
    if cls.empty_allocation(allocationMode):
      CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_compute)
      print('  Failed. Allocation was not in use. Actual allocationMode was: %s' % allocationMode)
      return
    if allocationMode['instances'] == GeneralConfigurations.max_computes:
      print('  Ok. Removing compute')
    else:
      print('  Failed. Removing compute')
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_compute)


  @classmethod
  def get_allocation(cls, member):
    response = requests.get(CommonMethods.url_computes + GeneralConfigurations.allocation_endpoint + member, headers= GeneralConfigurations.json_header)
    return response

  @classmethod
  def empty_allocation(cls, allocationMode):
    if allocationMode['vCPU'] == 0 and allocationMode['memory'] == 0 and allocationMode['instances'] == 0:
      return True
    return False

  # Get tests
  @classmethod
  def test_get_by_id_compute(cls, data):
    extra_data = {}
    extra_data.update(data)
    fake_id = 'fake-id'
    member = extra_data[GeneralConfigurations.provider] if GeneralConfigurations.provider in extra_data else GeneralConfigurations.local_member
    if not CommonMethods.wait_compute_available(GeneralConfigurations.max_computes, member):
      print('  Failed. There is not %d instances available.' % GeneralConfigurations.max_computes)
      return
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_compute)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('  Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    order_id = CommonMethods.post_compute(extra_data)
    if not order_id:
      print('  Failed when creating compute, trying next test')
      CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
      #time to wait order to be deleted
      time.sleep(20)
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    response_get = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
    if response_get.status_code == GeneralConfigurations.ok_status:
      print('  Ok. Removing compute')
    else:
      print('  Failed. Expecting %d status, but got: %d. Removing compute' % (GeneralConfigurations.ok_status, response_get.status_code))
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
    #time to wait order to be deleted
    time.sleep(20)


  @classmethod
  def test_get_all_compute(cls, data):
    extra_data = {}
    extra_data.update(data)
    member = extra_data[GeneralConfigurations.provider] if GeneralConfigurations.provider in extra_data else GeneralConfigurations.local_member
    if not CommonMethods.wait_compute_available(GeneralConfigurations.max_computes, member):
      print('  Failed. There is not %d instances available.' % GeneralConfigurations.max_computes)
      return
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_compute)
    if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
      print('  Failed. Wrong status in get request, got status: %d, and message: %s' % (response_get.status_code, response_get.text))
      return
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_computes, GeneralConfigurations.type_compute)
    if not orders_id:
      print('  Failed. Could not create computes')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_compute)
    test_ok = not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_computes)
    if test_ok:
      print('  Ok. Removing computes')
    else:
      print('  Failed. Removing computes')
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
  def test_delete_compute(cls, data):
    extra_data = {}
    extra_data.update(data)
    order_id = CommonMethods.post_compute(extra_data)
    if not order_id:
      print('  Failed. Could not create a compute')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
    if (get_response.status_code != GeneralConfigurations.ok_status):
      print(get_response.status_code)
      CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
      #time to wait order to be deleted
      time.sleep(20)
      print('  Failed. Could not get order by id')
      return
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait delete request to be received
      time.sleep(10)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print(' Failed. Expected %d HTTP status, but got: %d' % (GeneralConfigurations.not_found_status, get_response.status_code))
      return
    print('  Ok. Compute removed')
    #time to wait order to be deleted
    time.sleep(20)