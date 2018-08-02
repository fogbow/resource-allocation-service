import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class VolumeTests:

  @classmethod
  def test_volumes(cls):
    cls.test_post_local_volumes()
    cls.test_get_by_id_local_volume()
    cls.test_get_all_local_compute()
    cls.test_delete_local_volume()

  #Post tests
  @classmethod
  def test_post_local_volumes(cls):
    extra_data = {}
    order_id = CommonMethods.post_volume(extra_data)
    if not order_id:
      print('Test post local volume: Failed, trying next test')
      return
    if CommonMethods.wait_instance_ready(order_id, GeneralConfigurations.type_volume):
      print('Test post local volume: Ok. Removing volume')
    else:
      print('Test post local volume: Failed. Removing volume')
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_volume)

  # Get tests
  @classmethod
  def test_get_by_id_local_volume(cls):
    extra_data = {}
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_volume)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('Test get volume by id: Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    order_id = CommonMethods.post_volume(extra_data)
    if not order_id:
      print('Test get by id: Failed on creating volume, trying next test')
      return
    response_get = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_volume)
    if response_get.status_code == GeneralConfigurations.ok_status:
      print('Test get volume by id: Ok. Removing volume')
    else:
      print('Test get volume by id: Failed. Expecting %d status, but got: %d. Removing volume' % (GeneralConfigurations.ok_status, response_get.status_code))
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_volume)

  @classmethod
  def test_get_all_local_compute(cls):
    time.sleep(60)
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_volume)
    if response_get.status_code != GeneralConfigurations.ok_status or response_get.text != '[]':
      print('Test get all volumes: Failed')
      return
    extra_data = {}
    orders_id = CommonMethods.post_multiple_orders(extra_data, GeneralConfigurations.max_volumes, GeneralConfigurations.type_volume)
    if not orders_id:
      print('Test get all volumes: Failed. Could not create volumes')
      return
    response_get = CommonMethods.get_all_order(GeneralConfigurations.type_volume)
    test_ok = not (response_get.status_code != GeneralConfigurations.ok_status or response_get.text == '[]' or len(response_get.json()) != GeneralConfigurations.max_volumes)
    if test_ok:
      print('Test get all local compute: Ok. Removing volumes')
    else:
      print('Test get all volumes: Failed. Removing volumes')
    CommonMethods.delete_multiple_orders(orders_id, GeneralConfigurations.type_volume)

  # Delete tests
  @classmethod
  def test_delete_local_volume(cls):
    extra_data = {}
    order_id = CommonMethods.post_volume(extra_data)
    if not order_id:
      print('Test Failed. Trying next test')
      return
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_volume)
    if (get_response.status_code == GeneralConfigurations.not_found_status):
      print('Test Failed. Trying next test')
      return
    CommonMethods.delete_order(order_id, GeneralConfigurations.type_volume)
    get_response = CommonMethods.get_order_by_id(order_id, GeneralConfigurations.type_volume)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print('Test delete local volume: Failed.')
      return
    print('Test delete local volume: Ok. Volume removed')