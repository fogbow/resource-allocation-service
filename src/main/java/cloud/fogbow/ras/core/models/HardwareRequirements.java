package cloud.fogbow.ras.core.models;

public class HardwareRequirements implements Comparable<HardwareRequirements> {
    /**
     * the lower value, the greater relevance.
     */
    private final int VCPU_VALUE_RELEVANCE = 1;
    private final int MEM_VALUE_RELEVANCE = 1;
    private String name;
    private String flavorId;
    /**
     * Number of cores of the CPU.
     */
    private int cpu;
    /**
     * RAM memory in MB.
     */
    private int memory;
    /**
     * Disk in GB.
     */
    private int disk;

    public HardwareRequirements(String name, String flavorId, int cpu, int memory, int disk) {
        this.setName(name);
        this.setCpu(cpu);
        this.setMemory(memory);
        this.setDisk(disk);
        this.setFlavorId(flavorId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlavorId() {
        return flavorId;
    }

    public void setFlavorId(String flavorId) {
        this.flavorId = flavorId;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public int getDisk() {
        return disk;
    }

    public void setDisk(int disk) {
        this.disk = disk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HardwareRequirements hardwareRequirements = (HardwareRequirements) o;

        return flavorId != null ? flavorId.equals(hardwareRequirements.flavorId) : hardwareRequirements.flavorId == null;
    }

    @Override
    public int hashCode() {
        return flavorId != null ? flavorId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Name: " + getName() + ", cpu: " + cpu + ", mem: " + memory + ", disk: " + disk;
    }

    @Override
    public int compareTo(HardwareRequirements hardwareRequirements) {
        double oneRelevance = calculateRelevance(this, hardwareRequirements);
        double twoRelevance = calculateRelevance(hardwareRequirements, this);

        if (oneRelevance != twoRelevance) {
            return Double.compare(oneRelevance, twoRelevance);
        }

        int oneDisk = this.getDisk();
        int twoDisk = hardwareRequirements.getDisk();
        return Integer.compare(oneDisk, twoDisk);
    }

    private double calculateRelevance(HardwareRequirements req1, HardwareRequirements req2) {
        int cpu1 = req1.getCpu();
        int cpu2 = req2.getCpu();
        int ram1 = req1.getMemory();
        int ram2 = req2.getMemory();

        return ((cpu1 / cpu2) / VCPU_VALUE_RELEVANCE) + ((ram1 / ram2) / MEM_VALUE_RELEVANCE);
    }
}
