import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class AttachmentTests:

  compute_id = ''
  volume_id = ''
  attachment_id = ''

  @classmethod
  def test_attachments(cls):
    cls.test_post_local_attachment()
    cls.test_get_by_id_local_attachment()
    cls.test_delete_local_attachment()
    cls.test_get_all_local_attachment()

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
  def test_post_local_attachment(cls):
    if not cls.__post_attachment():
      print('Test post local attachment: Failed.')
      return
    if CommonMethods.wait_instance_ready(cls.attachment_id, GeneralConfigurations.type_attachment):
      print('Test post local attachment: Ok. Removing attachment')
    else:
      print('Test post local attachment: Failed. Removing attachment')
    cls.__delete_all_orders()

  #Get tests
  @classmethod
  def test_get_by_id_local_attachment(cls):
    extra_data = {}
    fake_id = 'fake-id'
    response_get = CommonMethods.get_order_by_id(fake_id, GeneralConfigurations.type_attachment)
    if response_get.status_code != GeneralConfigurations.not_found_status:
      print('Test get attachment by id: Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.not_found_status, response_get.status_code))
      return
    if not cls.__post_attachment():
      print('Test get attachment by id: Failed.')
      return
    response_get = CommonMethods.get_order_by_id(cls.attachment_id, GeneralConfigurations.type_attachment)
    test_ok = False
    if response_get.status_code == GeneralConfigurations.ok_status:
      test_ok = True
    if test_ok:
      print('Test get attachment by id: Ok. Removing attachment')
    else:
      print('Test get attachment by id: Failed. Expecting %d status, but got: %d. Removing attachment' % (GeneralConfigurations.ok_status, response_get.status_code))
    cls.__delete_all_orders()

  #This function creates only 1 order, ideally it should be more than 1
  @classmethod
  def test_get_all_local_attachment(cls):
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_attachment)
    if response_get_all.status_code != GeneralConfigurations.ok_status:
      print('Test get all attachment: Failed. Expecting %d status, but got: %d' % (GeneralConfigurations.ok_status, response_get_all.status_code))
      return
    if not cls.__post_attachment():
      print('Test get all attachment: Failed.')
      return
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_attachment)
    test_ok = not (response_get_all.status_code != GeneralConfigurations.ok_status or response_get_all.text == '[]' or len(response_get_all.json()) != 1)
    if test_ok:
      print('Test get all attachment: Ok. Removing attachment')
    else:
      print('Test get all attachment: Failed. Expecting %d status, but got: %d. Removing attachment' % (GeneralConfigurations.ok_status, response_get_all.status_code))
    cls.__delete_all_orders()

  #Delete methods
  @classmethod
  def test_delete_local_attachment(cls):
    if not cls.__post_attachment():
      print('Test delete local attachment: Failed.')
      return
    CommonMethods.delete_order(cls.attachment_id, GeneralConfigurations.type_attachment)
    get_response = CommonMethods.get_order_by_id(cls.attachment_id, GeneralConfigurations.type_attachment)
    if (get_response.status_code != GeneralConfigurations.not_found_status):
      print('Test delete local attachment: Failed.')
    else:
      print('Test delete local attachment: Ok. Attachment removed')
    cls.__delete_all_orders()

  @classmethod
  def __post_attachment(cls):
    empty_extra_data = {}
    cls.compute_id = CommonMethods.post_compute(empty_extra_data)
    if not cls.compute_id:
      print('Error while trying to create compute')
      return
    cls.volume_id = CommonMethods.post_volume(empty_extra_data)
    if not cls.volume_id:
      CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
      print('Error while trying to create volume')
      return
    if not cls.wait_for_compute_and_volume(cls.compute_id, cls.volume_id):
      CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
      CommonMethods.delete_order(cls.volume_id, GeneralConfigurations.type_volume)
      print('Volume or compute was not transitioned to ready')
      return
    attachment_extra_data = {GeneralConfigurations.source: cls.compute_id, GeneralConfigurations.target: cls.volume_id}
    cls.attachment_id = CommonMethods.post_attachment(attachment_extra_data)
    if not cls.attachment_id:
      CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
      CommonMethods.delete_order(cls.volume_id, GeneralConfigurations.type_volume)
      print('Error while trying to create attachment')
      return
    return True

  @classmethod
  def __delete_all_orders(cls):
    CommonMethods.delete_order(cls.attachment_id, GeneralConfigurations.type_attachment)
    CommonMethods.delete_order(cls.compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(cls.volume_id, GeneralConfigurations.type_volume)
    time.sleep(10)