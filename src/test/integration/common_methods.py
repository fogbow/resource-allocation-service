import requests
import time
from general_configuration_test import GeneralConfigurations

class CommonMethods:

  url_computes= GeneralConfigurations.base_url + GeneralConfigurations.computes_endpoint
  url_networks= GeneralConfigurations.base_url + GeneralConfigurations.networks_endpoint
  url_volumes= GeneralConfigurations.base_url + GeneralConfigurations.volumes_endpoint
  url_attachments= GeneralConfigurations.base_url + GeneralConfigurations.attachments_endpoint

  data_compute = {'vCPU': GeneralConfigurations.vCPU, 'memory': GeneralConfigurations.memory, 'disk': GeneralConfigurations.disk, 'providingMember': GeneralConfigurations.local_member, 'imageId': GeneralConfigurations.imageId, 'publicKey': GeneralConfigurations.publicKey}
  data_network = {'address': GeneralConfigurations.address, 'allocation': GeneralConfigurations.allocation}
  data_volume = {'volumeSize': GeneralConfigurations.volume_size}
  data_attachment = {'device':'/dev/sdd'}

  @classmethod
  def wait_instance_ready(cls, order_id, order_type):
    state_key = 'state'
    ready_state = 'READY'
    for x in range(GeneralConfigurations.max_tries + 1):
      response = cls.get_order_by_id(order_id, order_type)
      json_response = response.json()
      if json_response[state_key] != ready_state:
        if(x < GeneralConfigurations.max_tries):
          time.sleep(GeneralConfigurations.sleep_time_secs)
          continue
        return False
      break
    return True

  @classmethod
  def post_order(cls, optional_attributes, order_type):
    if order_type == GeneralConfigurations.type_compute:
      merged_data = cls.data_compute.copy()
      merged_data.update(optional_attributes)
      response = requests.post(cls.url_computes, json= merged_data, headers= GeneralConfigurations.json_header)
      return response
    elif order_type == GeneralConfigurations.type_network:
      merged_data = cls.data_network.copy()
      merged_data.update(optional_attributes)
      response = requests.post(cls.url_networks, json= merged_data, headers= GeneralConfigurations.json_header)
      return response
    elif order_type == GeneralConfigurations.type_volume:
      merged_data = cls.data_volume.copy()
      merged_data.update(optional_attributes)
      response = requests.post(cls.url_volumes, json= merged_data, headers= GeneralConfigurations.json_header)
      return response
    elif order_type == GeneralConfigurations.type_attachment:
      merged_data = cls.data_attachment.copy()
      merged_data.update(optional_attributes)
      response = requests.post(cls.url_attachments, json= merged_data, headers= GeneralConfigurations.json_header)
      return response

  @classmethod
  def post_multiple_orders(cls, optional_attributes, amount, order_type):
    orders_id = []
    for i in range(amount):
      response = cls.post_order(optional_attributes, order_type)
      if response.status_code != GeneralConfigurations.created_status:
        print('Test get all computes: Failed on creating compute, trying next test')
        return
      orders_id.append(response.text)
    return orders_id

  @classmethod
  def get_order_by_id(cls, order_id, order_type):
    if order_type == GeneralConfigurations.type_compute:
      return requests.get(cls.url_computes + order_id)
    elif order_type == GeneralConfigurations.type_network:
      return requests.get(cls.url_networks + order_id)
    elif order_type == GeneralConfigurations.type_volume:
      return requests.get(cls.url_volumes + order_id)
    elif order_type == GeneralConfigurations.type_attachment:
      return requests.get(cls.url_attachments + order_id)

  @classmethod
  def get_all_order(cls, order_type):
    if order_type == GeneralConfigurations.type_compute:
      return requests.get(cls.url_computes + GeneralConfigurations.status_endpoint)
    elif order_type == GeneralConfigurations.type_network:
      return requests.get(cls.url_networks + GeneralConfigurations.status_endpoint)
    elif order_type == GeneralConfigurations.type_volume:
      return requests.get(cls.url_volumes + GeneralConfigurations.status_endpoint)
    elif order_type == GeneralConfigurations.type_attachment:
      return requests.get(cls.url_attachments + GeneralConfigurations.status_endpoint)

  @classmethod
  def delete_order(cls, order_id, order_type):
    if order_type == GeneralConfigurations.type_compute:
      return requests.delete(cls.url_computes + order_id)
    elif order_type == GeneralConfigurations.type_network:
      return requests.delete(cls.url_networks + order_id)
    elif order_type == GeneralConfigurations.type_volume:
      return requests.delete(cls.url_volumes + order_id)
    elif order_type == GeneralConfigurations.type_attachment:
      return requests.delete(cls.url_attachments + order_id)

  @classmethod
  def delete_multiple_orders(cls, orders_id, order_type):
    for id in orders_id:
      cls.delete_order(id, order_type)