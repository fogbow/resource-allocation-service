package org.fogbowcloud.manager.core.models;

public class HardwareRequirements implements Comparable<HardwareRequirements> {
    
    /** the lower value, the greater relevance. */
    private final int VCPU_VALUE_RELEVANCE = 1;
    private final int MEM_VALUE_RELEVANCE = 1;

    private String name;
    private String id;

    /** Number of cores of the CPU. */
    private int cpu;
    /** RAM memory in MB. */
    private int ram;
    /** Disk in GB. */
    private int disk;

    public HardwareRequirements(String name, String id, int cpu, int ram, int disk) {
        this.setName(name);
        this.setCpu(cpu);
        this.setRam(ram);
        this.setDisk(disk);
        this.setId(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
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

        return id != null ? id.equals(hardwareRequirements.id) : hardwareRequirements.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Name: " + getName() + ", cpu: " + cpu + ", mem: " + ram + ", disk: " + disk;
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
        int ram1 = req1.getRam();
        int ram2 = req2.getRam();

        return ((cpu1 / cpu2) / VCPU_VALUE_RELEVANCE) + ((ram1 / ram2) / MEM_VALUE_RELEVANCE);
    }
}
