package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

public enum AwsFlavor {

	T1_MICRO("t1.micro") {

		private final int VCPU_CORE_VALUE = 1;
		private final double MEMORY_VALUE = 0.613; 
		
		@Override
		public Integer getVCpu() {
			return VCPU_CORE_VALUE;
		}

		@Override
		public Double getMemory() {
			return MEMORY_VALUE;
		}

	};

	private String name;

	AwsFlavor(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract Integer getVCpu();

	public abstract Double getMemory();

}
