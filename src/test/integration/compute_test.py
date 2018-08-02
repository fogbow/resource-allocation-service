import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class ComputeTests:

  @classmethod
  def test_computes(cls):
    cls.test_post_local_compute()
    cls.test_delete_local_compute()
    cls.test_get_all_local_compute()
    cls.test_get_by_id_local_compute()
    cls.test_local_quota()
    cls.test_local_allocation()

  # Post tests
  @classmethod
  def test_post_local_compute(cls):
    extra_data = {}
    order_id = CommonMethods.post_compute(extra_data)
    if not order_id:
      print('Test post local compute: Failed, trying next test')
      return 
    if cls.wait_instance_ready(order_id):
      print('Test post local compute: Ok. Removing compute')
    else:
      print('Test post local compute: Failed. Removing compute')
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_compute)

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
      print('Test get all computes: Failed. There is not %d instances available.')
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
      cls.wait_instance_ready(order)
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
      print('Test get all computes: Failed. There is not %d instances available.')
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
  def wait_instance_ready(cls, order_id):
    state_key = 'state'
    ready_state = 'READY'
    for x in range(GeneralConfigurations.max_tries + 1):
      response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_compute)
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