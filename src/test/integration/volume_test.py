import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class VolumeTests:

  test_number = 0

  @classmethod
  def test_volumes(cls):
    data_for_local = {}
    print('-Test %d: post local volume' % cls.which_test_case())
    cls.test_post_volumes(data_for_local)
    print('-Test %d: get by id local volume' % cls.which_test_case())
    cls.test_get_by_id_volume(data_for_local)
    print('-Test %d: get all local volume' % cls.which_test_case())
    cls.test_get_all_volume(data_for_local)
    print('-Test %d: delete local volume' % cls.which_test_case())
    cls.test_delete_volume(data_for_local)
    if GeneralConfigurations.remote_member:
      data_for_remote = {GeneralConfigurations.provider: GeneralConfigurations.remote_member}
      print('-Test %d: post remote volume' % cls.which_test_case())
      cls.test_post_volumes(data_for_remote)
      print('-Test %d: get by id remote volume' % cls.which_test_case())
      cls.test_get_by_id_volume(data_for_remote)
      print('-Test %d: get all remote volume' % cls.which_test_case())
      cls.test_get_all_volume(data_for_remote)
      print('-Test %d: delete remote volume' % cls.which_test_case())
      cls.test_delete_volume(data_for_remote)

  @classmethod
  def which_test_case(cls):
    cls.test_number += 1
    return cls.test_number

  #Post tests
  @classmethod
  def test_post_volumes(cls, data):
    extra_data = {}
    extra_data.update(data)
    order_id = CommonMethods.post_volume(extra_data)
    if not order_id:
      print('  Failed, volume was not created')
      return
    if CommonMethods.wait_instance_ready(order_id, GeneralConfigurations.type_volume):
      print('  Ok. Removing volume')
    else:
      print('  Failed. Removing volume')
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_volume)

  # Get tests
  @classmethod
  def test_get_by_id_volume(cls, data):
    extra_data = {}
    extra_data.update(data)
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_volume)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('  Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    order_id = CommonMethods.post_volume(extra_data)
    if not order_id:
      print('  Failed on creating volume, trying next test')
      return
    if GeneralConfigurations.provider in data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)
    response_get = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_volume)
    if response_get.status_code == GeneralConfigurations.ok_status:
      print('  Ok. Removing volume')
    else:
      print('  Failed. Expecting %d status, but got: %d. Removing volume' % (GeneralConfigurations.ok_status, response_get.status_code))
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_volume)
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)

  @classmethod
  def test_get_all_volume(cls, data):
    extra_data = {}
    extra_data.update(data)
    time.sleep(60)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_volume)
    if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
      print('  Failed. There is a volume created already')
      return
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_volumes, GeneralConfigurations.type_volume)
    if not orders_id:
      print('  Failed. Could not create volumes')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_volume)
    test_ok = not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_volumes)
    if test_ok:
      print('  Ok. Removing volumes')
    else:
      print('  Failed. Got status %d and message: %s' % (response_get.status_code, response_get.text))
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_volume)
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)

  # Delete tests
  @classmethod
  def test_delete_volume(cls, data):
    extra_data = {}
    extra_data.update(data)
    order_id = CommonMethods.post_volume(extra_data)
    if not order_id:
      print('  Test Failed. Volume was not created')
      return
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_volume)
    if (get_response.status_code == GeneralConfigurations.not_found_status):
      print('  Test Failed. Volume was not found')
      return
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_volume)
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(10)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_volume)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print('  Failed. Expecting http status %d, but got: %d' % (GeneralConfigurations.not_found_status, get_response.status_code))
      return
    print('  Ok. Volume removed')
    if GeneralConfigurations.provider in extra_data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)