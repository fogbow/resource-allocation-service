import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class AttachmentTests:

  test_number = 0

  compute_id = ''
  volume_id = ''
  attachment_id = ''

  @classmethod
  def test_attachments(cls):
    data_for_local = {}
    print('-Test %d: post local attachment' % cls.which_test_case())
    cls.test_post_attachment(data_for_local)
    print('-Test %d: get by id local attachment' % cls.which_test_case())
    cls.test_get_by_id_attachment(data_for_local)
    print('-Test %d: get all local attachment' % cls.which_test_case())
    cls.test_get_all_attachment(data_for_local)
    print('-Test %d: delete local attachment' % cls.which_test_case())
    cls.test_delete_attachment(data_for_local)
    if GeneralConfigurations.remote_member:
      data_for_remote = {GeneralConfigurations.provider: GeneralConfigurations.remote_member}
      print('-Test %d: post remote attachment' % cls.which_test_case())
      cls.test_post_attachment(data_for_remote)
      print('-Test %d: get by id remote attachment' % cls.which_test_case())
      cls.test_get_by_id_attachment(data_for_remote)
      print('-Test %d: get all remote attachment' % cls.which_test_case())
      cls.test_get_all_attachment(data_for_remote)
      print('-Test %d: delete remote attachment' % cls.which_test_case())
      cls.test_delete_attachment(data_for_remote)

  @classmethod
  def which_test_case(cls):
    cls.test_number += 1
    return cls.test_number

  @classmethod
  def wait_for_compute_and_volume(cls, compute_id, volume_id):
    if not CommonMethods.wait_instance_ready(compute_id, GeneralConfigurations.type_compute):
      CommonMethods.delete_order(volume_id, GeneralConfigurations.type_volume)
      CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
      return False
    if not CommonMethods.wait_instance_ready(volume_id, GeneralConfigurations.type_volume):
      CommonMethods.delete_order(volume_id, GeneralConfigurations.type_volume)
      CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
      return False
    return True

  #Post tests
  @classmethod
  def test_post_attachment(cls, data):
    extra_data = {}
    extra_data.update(data)
    if not cls.__post_attachment(extra_data):
      print('  Failed')
      return
    if CommonMethods.wait_instance_ready(cls.attachment_id, GeneralConfigurations.type_attachment):
      print('  Ok. Removing attachment')
    else:
      print('  Failed. Attachment did not transitioned to \'ready\' state')
    cls.__delete_all_orders(extra_data)

  #Get tests
  @classmethod
  def test_get_by_id_attachment(cls, data):
    extra_data = {}
    extra_data.update(data)
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_attachment)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('  Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    if not cls.__post_attachment(extra_data):
      print('  Failed.')
      return
    if GeneralConfigurations.provider in data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)
    response_get = CommonMethods.get_order_by_id(cls.attachment_id, GeneralConfigurations.type_attachment)
    test_ok = False
    if response_get.status_code == GeneralConfigurations.ok_status:
      test_ok = True
    if test_ok:
      print('  Ok. Removing attachment')
    else:
      print('  Failed. Expecting %d status, but got: %d. Removing attachment' % (GeneralConfigurations.ok_status, response_get.status_code))
    cls.__delete_all_orders(extra_data)

  #This function creates only 1 order, ideally it should be more than 1
  @classmethod
  def test_get_all_attachment(cls, data):
    extra_data = {}
    extra_data.update(data)
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_attachment)
    if response_get_all.status_code != GeneralConfigurations.ok_status:
      print('  Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.ok_status, response_get_all.status_code))
      return
    if not cls.__post_attachment(extra_data):
      print('  Failed.')
      return
    if GeneralConfigurations.provider in data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_attachment)
    test_ok = not (response_get_all.status_code != GeneralConfigurations.ok_status or response_get_all.text == '[]' or len(response_get_all.json()) != GeneralConfigurations.max_attachment)
    if test_ok:
      print('  Ok. Removing attachment')
    else:
      print('  Failed. Received status: %d. Message: %s. Removing attachment' % (response_get_all.status_code, response_get_all.text))
    cls.__delete_all_orders(extra_data)

  #Delete methods
  @classmethod
  def test_delete_attachment(cls, data):
    extra_data = {}
    extra_data.update(data)
    if not cls.__post_attachment(extra_data):
      print('  Failed.')
      return
    CommonMethods.delete_order(cls.attachment_id, GeneralConfigurations.type_attachment)
    if GeneralConfigurations.provider in data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)
    get_response = CommonMethods.get_order_by_id(cls.attachment_id, GeneralConfigurations.type_attachment)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print('  Failed. Expecting http status %d, but got: %d' % (GeneralConfigurations.not_found_status, get_response.status_code))
    else:
      print('  Ok. Attachment removed')
    cls.__delete_all_orders(extra_data)

  @classmethod
  def __post_attachment(cls, data):
    extra_data = {}
    extra_data.update(data)
    cls.compute_id = CommonMethods.post_compute(extra_data)
    if not cls.compute_id:
      print('  Error while trying to create compute')
      return
    cls.volume_id = CommonMethods.post_volume(extra_data)
    if not cls.volume_id:
      CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
      print('  Error while trying to create volume')
      return
    if not cls.wait_for_compute_and_volume(cls.compute_id, cls.volume_id):
      CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
      CommonMethods.delete_order(cls.volume_id, GeneralConfigurations.type_volume)
      print('  Volume or compute was not transitioned to ready')
      return
    attachment_extra_data = {GeneralConfigurations.source: cls.compute_id, GeneralConfigurations.target: cls.volume_id}
    attachment_extra_data.update(data)
    cls.attachment_id = CommonMethods.post_attachment(attachment_extra_data)
    if not cls.attachment_id:
      CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
      CommonMethods.delete_order(cls.volume_id, GeneralConfigurations.type_volume)
      print('  Error while trying to create attachment')
      return
    return True

  @classmethod
  def __delete_all_orders(cls, data):
    CommonMethods.delete_order(cls.attachment_id, GeneralConfigurations.type_attachment)
    CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(cls.volume_id, GeneralConfigurations.type_volume)
    if GeneralConfigurations.provider in data:
      #if it is remote, we need to wait order request to be received
      time.sleep(30)
    else:
      time.sleep(10)