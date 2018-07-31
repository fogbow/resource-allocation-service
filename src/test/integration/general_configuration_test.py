class GeneralConfigurations:

	#deploy configurations
	base_url = 'http://localhost:8080/'
	local_member = 'fake-localidentity-member'
	remote_member = ''

	#general attributes
	json_header = {"Content-Type": "application/json"}
	status_endpoint = 'status'
	max_tries = 6
	sleep_time_secs = 15

	#http status
	ok_status = 200
	not_found_status = 404
	created_status = 201

	#order type
	compute = 'compute'
	network = 'network'
	volume = 'volume'

	#compute attributes
	computes_endpoint = 'computes/'
	quota_endpoint = 'quota/'
	allocation_endpoint = 'allocation/'
	available_quota = 'availableQuota'
	instances_quota = 'instances'
	max_computes = 2
	#compute requirements
	vCPU = 1
	memory = 1024
	disk = 8
	publicKey = 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCYBZoM6Za/x02OWdPOElGmuEevpCCAeObDxufVlqNKW7EpYlrEend9ulzvz6f2FVxM7NqLAskZ0tElBf4vnCPdwhkVB0pWxc8NReZ+B2eA7gdUXn0t8mDiCybOkAcoTbxMO88cyo/f1e7g+AxxGzeg5FwKjcQIr25u3xn/m2ICJO0Jzflyxx/Hu9d2uFBqA1UHOLJRSUT95NXWZoHhZS+KFHZsh2IFHpBapaE1CloYObfL+nYCB6fMlu457LUUpkghk4lqWQhL0WaElmUsevQJ/Z8KVcJVNkCyq0iAHA1jO8VuFnTHxEB0J4+kVF6yebOghOexu5QvOci8E/cn6+sr gustavolocal@sapupara-pc-154'
	imageId = '9b672abd-67f7-463e-b926-a87adbc80860'

	#network attributes
	networks_endpoint = 'networks/'
	address = "10.15.20.1/28"
	allocation = "dynamic"

	#volume attributes
	volumes_endpoint = 'volumes/'