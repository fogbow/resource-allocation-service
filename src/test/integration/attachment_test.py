import requests
import time
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class AttachmentTests:

  @classmethod
  def test_attachments(cls):
    cls.test_post_local_attachments()

  #Post tests
  @classmethod
  def test_post_local_attachments(cls):
    empty_extra_data = {}
    compute_id = cls.post_compute(empty_extra_data)
    if not compute_id:
      print('Test post local attachment: Failed. Compute not created')
      return
    volume_id = cls.post_volume(empty_extra_data)
    if not volume_id:
      CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
      print('Test post local attachment: Failed. Volume not created')
      return
    if not cls.wait_for_compute_and_volume(compute_id, volume_id):
      print('Test post local attachment: Failed. Volume or compute not ready')
      return
    attachment_extra_data = {GeneralConfigurations.source: compute_id, GeneralConfigurations.target: volume_id}
    attachment_id = cls.post_attachment(attachment_extra_data)
    if not attachment_id:
      CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
      CommonMethods.delete_order(volume_id, GeneralConfigurations.type_volume)
      print('Test post local attachment: Failed. Attachment and not created')
      return
    if CommonMethods.wait_instance_ready(attachment_id, GeneralConfigurations.type_attachment):
      print('Test post local attachment: Ok. Removing attachment')
    else:
      print('Test post local attachment: Failed. Removing attachment')
    CommonMethods.delete_order(attachment_id, GeneralConfigurations.type_attachment)
    CommonMethods.delete_order(compute_id, GeneralConfigurations.type_compute)
    CommonMethods.delete_order(volume_id, GeneralConfigurations.type_volume)

  @classmethod
  def post_compute(cls, extra_data):
    response_compute = CommonMethods.post_order(extra_data, GeneralConfigurations.type_compute)
    if response_compute.status_code != GeneralConfigurations.created_status:
      print('Test post local attachment: Failed, trying next test')
      return
    return response_compute.text

  @classmethod
  def post_volume(cls, extra_data):
    response_volume = CommonMethods.post_order(extra_data, GeneralConfigurations.type_volume)
    if response_volume.status_code != GeneralConfigurations.created_status:
      print('Test post local attachment: Failed, trying next test')
      return
    return response_volume.text

  @classmethod
  def post_attachment(cls, extra_data):
    response_attachment = CommonMethods.post_order(extra_data, GeneralConfigurations.type_attachment)
    if response_attachment.status_code != GeneralConfigurations.created_status:
      print('Test post local attachment: Failed, trying next test')
      return
    return response_attachment.text

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