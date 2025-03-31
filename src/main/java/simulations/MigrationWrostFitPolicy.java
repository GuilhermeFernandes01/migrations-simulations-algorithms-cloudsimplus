package simulations;

import ch.qos.logback.classic.Level;
import simulations.Shared.Config;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.MipsShare;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModel;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.CsvTable;
import org.cloudsimplus.builders.tables.HostHistoryTableBuilder;
import org.cloudsimplus.listeners.DatacenterBrokerEventInfo;
import org.cloudsimplus.listeners.VmHostEventInfo;
import org.cloudsimplus.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingLong;

import java.io.IOException;
import java.io.PrintStream;

public final class MigrationWrostFitPolicy {
  private final List<Vm> vmList = new ArrayList<>();
  private final DatacenterBrokerSimple broker;

  private final CloudSimPlus simulation;
  private List<Host> hostList;
  private int migrationsNumber = 0;

  public static void main(String[] args) {
    new MigrationWrostFitPolicy();
  }

  private MigrationWrostFitPolicy() {
    Log.setLevel(Level.INFO);

    if (Config.Host.PES.length != Config.Host.RAM.length) {
      throw new IllegalStateException("The length of arrays Config.Host.PES and Config.Host.RAM must match.");
    }

    System.out.println("Starting " + getClass().getSimpleName());
    simulation = new CloudSimPlus();

    @SuppressWarnings("unused")
    final Datacenter datacenter0 = createDatacenter();
    broker = new DatacenterBrokerSimple(simulation);
    createAndSubmitVms(broker);
    createAndSubmitCloudlets(broker);

    broker.addOnVmsCreatedListener(this::onVmsCreatedListener);

    simulation.start();

    final List<Cloudlet> finishedList = broker.getCloudletFinishedList();
    final Comparator<Cloudlet> cloudletComparator = comparingLong((Cloudlet c) -> c.getVm().getHost().getId())
        .thenComparingLong(c -> c.getVm().getId());
    finishedList.sort(cloudletComparator);

    try {
      CsvTable csv = new CsvTable();
      csv.setPrintStream(new PrintStream(new java.io.File("results/migration_worst_fit_policy.csv")));
      new CloudletsTableBuilder(finishedList, csv).build();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    System.out.printf(
        "%nHosts CPU usage History (when the allocated MIPS is lower than the requested, it is due to VM migration overhead)%n");

    hostList.stream().filter(h -> h.getId() <= 2).forEach(this::printHostStateHistory);
    System.out.printf("Number of VM migrations: %d%n", migrationsNumber);
    System.out.println(getClass().getSimpleName() + " finished!");
  }

  private void startMigration(final VmHostEventInfo info) {
    final Vm vm = info.getVm();
    final Host targetHost = info.getHost();
    System.out.printf(
        "# %.2f: %s started migrating to %s (you can perform any operation you want here)%n",
        info.getTime(), vm, targetHost);
    showVmAllocatedMips(vm, targetHost, info.getTime());

    // VM current host (source)
    showHostAllocatedMips(info.getTime(), vm.getHost());

    // Migration host (target)
    showHostAllocatedMips(info.getTime(), targetHost);
    System.out.println();

    migrationsNumber++;
    if (migrationsNumber > 1) {
      return;
    }

    // After the first VM starts being migrated, tracks some metrics along
    // simulation time
    simulation.addOnClockTickListener(clock -> {
      if (clock.getTime() <= 2 || (clock.getTime() >= 11 && clock.getTime() <= 15))
        showVmAllocatedMips(vm, targetHost, clock.getTime());
    });
  }

  private void showVmAllocatedMips(final Vm vm, final Host targetHost, final double time) {
    final String msg = String.format("# %.2f: %s in %s: total allocated", time, vm, targetHost);
    final MipsShare allocatedMips = targetHost.getVmScheduler().getAllocatedMips(vm);
    final String msg2 = allocatedMips.totalMips() == Config.VM.MIPS * 0.9 ? " - reduction due to migration overhead"
        : "";
    System.out.printf("%s %.0f MIPs (divided by %d PEs)%s\n", msg, allocatedMips.totalMips(), allocatedMips.pes(),
        msg2);
  }

  private void finishMigration(final VmHostEventInfo info) {
    final Host host = info.getHost();
    System.out.printf(
        "# %.2f: %s finished migrating to %s (you can perform any operation you want here)%n",
        info.getTime(), info.getVm(), host);
    System.out.print("\t\t");
    showHostAllocatedMips(info.getTime(), hostList.get(1));
    System.out.print("\t\t");
    showHostAllocatedMips(info.getTime(), host);
  }

  private void showHostAllocatedMips(final double time, final Host host) {
    System.out.printf(
        "%.2f: %s allocated %.2f MIPS from %.2f total capacity%n",
        time, host, host.getTotalAllocatedMips(), host.getTotalMipsCapacity());
  }

  private void printHostStateHistory(final Host host) {
    new HostHistoryTableBuilder(host).setTitle(host.toString()).build();
  }

  public void createAndSubmitCloudlets(DatacenterBroker broker) {
    final List<Cloudlet> list = new ArrayList<>(Config.VM.PES.length);
    Cloudlet cloudlet = Cloudlet.NULL;
    UtilizationModelDynamic um = createCpuUtilizationModel(Config.Cloudlet.INITIAL_CPU_PERCENTAGE, 1);
    for (Vm vm : vmList) {
      cloudlet = createCloudlet(vm, broker, um);
      list.add(cloudlet);
    }

    // Changes the CPU usage of the last cloudlet to start at a lower value and
    // increase dynamically up to 100%
    cloudlet.setUtilizationModelCpu(createCpuUtilizationModel(0.2, 1));

    broker.submitCloudletList(list);
  }

  public Cloudlet createCloudlet(Vm vm, DatacenterBroker broker, UtilizationModel cpuUtilizationModel) {
    final UtilizationModel utilizationModelFull = new UtilizationModelFull();

    final Cloudlet cloudlet = new CloudletSimple(Config.Cloudlet.LENGTH, (int) vm.getPesNumber())
        .setFileSize(Config.Cloudlet.FILESIZE)
        .setOutputSize(Config.Cloudlet.OUTPUTSIZE)
        .setUtilizationModelRam(utilizationModelFull)
        .setUtilizationModelBw(utilizationModelFull)
        .setUtilizationModelCpu(cpuUtilizationModel);
    broker.bindCloudletToVm(cloudlet, vm);

    return cloudlet;
  }

  public void createAndSubmitVms(DatacenterBroker broker) {
    final List<Vm> list = new ArrayList<>(Config.VM.PES.length);
    for (final int pes : Config.VM.PES) {
      list.add(createVm(pes));
    }

    vmList.addAll(list);
    broker.submitVmList(list);

    list.forEach(vm -> vm.addOnMigrationStartListener(this::startMigration));
    list.forEach(vm -> vm.addOnMigrationFinishListener(this::finishMigration));
  }

  public Vm createVm(final int pes) {
    Vm vm = new VmSimple(Config.VM.MIPS, pes);
    vm
        .setRam(Config.VM.RAM).setBw((long) Config.VM.BW).setSize(Config.VM.SIZE)
        .setCloudletScheduler(new CloudletSchedulerTimeShared());

    return vm;
  }

  private UtilizationModelDynamic createCpuUtilizationModel(double initialCpuUsagePercent,
      double maxCpuUsagePercentage) {
    if (maxCpuUsagePercentage < initialCpuUsagePercent) {
      throw new IllegalArgumentException("Max CPU usage must be equal or greater than the initial CPU usage.");
    }

    initialCpuUsagePercent = Math.min(initialCpuUsagePercent, 1);
    maxCpuUsagePercentage = Math.min(maxCpuUsagePercentage, 1);
    final UtilizationModelDynamic utilizationModel;
    if (initialCpuUsagePercent < maxCpuUsagePercentage) {
      utilizationModel = new UtilizationModelDynamic(initialCpuUsagePercent)
          .setUtilizationUpdateFunction(this::getCpuUsageIncrement);
    } else
      utilizationModel = new UtilizationModelDynamic(initialCpuUsagePercent);

    utilizationModel.setMaxResourceUtilization(maxCpuUsagePercentage);
    return utilizationModel;
  }

  private double getCpuUsageIncrement(final UtilizationModelDynamic utilizationModel) {
    return utilizationModel.getUtilization()
        + utilizationModel.getTimeSpan() * Config.Cloudlet.CPU_INCREMENT_PER_SECOND;
  }

  private Datacenter createDatacenter() {
    this.hostList = createHosts();
    System.out.println();

    final Datacenter dc = new DatacenterSimple(simulation, hostList);
    for (Host host : hostList) {
      System.out.printf(
          "# Created %s with %.0f MIPS x %d PEs (%.0f total MIPS)%n",
          host, host.getMips(), host.getPesNumber(), host.getTotalMipsCapacity());
    }

    dc.setSchedulingInterval(Config.Scheduling.INTERVAL)
        .setHostSearchRetryDelay(Config.Host.SEARCH_RETRY_DELAY);

    return dc;
  }

  private List<Host> createHosts() {
    final List<Host> list = new ArrayList<>(Config.Host.PES.length);

    for (int i = 0; i < Config.Host.PES.length; i++) {
      final int pes = Config.Host.PES[i];
      final long ram = Config.Host.RAM[i];
      list.add(createHost(pes, ram));
    }

    return list;
  }

  public Host createHost(final int pesNumber, final long ram) {
    final List<Pe> peList = createPeList(pesNumber);
    final Host host = new HostSimple(ram, Config.Host.BW, Config.Host.STORAGE, peList);
    host.setVmScheduler(new VmSchedulerTimeShared());
    host.setStateHistoryEnabled(true);

    return host;
  }

  public List<Pe> createPeList(final int pesNumber) {
    final List<Pe> list = new ArrayList<>(pesNumber);
    for (int i = 0; i < pesNumber; i++) {
      list.add(new PeSimple(Config.Host.MIPS));
    }

    return list;
  }

  private void onVmsCreatedListener(final DatacenterBrokerEventInfo info) {
    System.out.printf("# All %d VMs submitted to the broker have been created.%n",
        broker.getVmCreatedList().size());
    broker.removeOnVmsCreatedListener(info.getListener());
    vmList.forEach(vm -> showVmAllocatedMips(vm, vm.getHost(), info.getTime()));

    System.out.println();
    hostList.forEach(host -> showHostAllocatedMips(info.getTime(), host));
    System.out.println();
  }
}
