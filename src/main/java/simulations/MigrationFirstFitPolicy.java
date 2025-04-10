package simulations;

import ch.qos.logback.classic.Level;
import simulations.Shared.Config;

import org.cloudsimplus.allocationpolicies.migration.VmAllocationPolicyMigrationFirstFitStaticThreshold;
import org.cloudsimplus.allocationpolicies.migration.VmAllocationPolicyMigrationStaticThreshold;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.selectionpolicies.VmSelectionPolicyMinimumUtilization;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.CsvTable;
import org.cloudsimplus.listeners.DatacenterBrokerEventInfo;
import org.cloudsimplus.listeners.VmHostEventInfo;
import org.cloudsimplus.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingLong;

import java.io.IOException;
import java.io.PrintStream;

public final class MigrationFirstFitPolicy {
  private final List<Vm> vmList = new ArrayList<>();
  private final DatacenterBrokerSimple broker;

  private final CloudSimPlus simulation;
  private VmAllocationPolicyMigrationStaticThreshold allocationPolicy;
  private List<Host> hostList;
  private final int[] migrationsNumber = {0};

  public static void main(String[] args) {
    new MigrationFirstFitPolicy();
  }

  private MigrationFirstFitPolicy() {
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
        .thenComparingLong(c -> c.getVm().getId())
        .thenComparingLong(Cloudlet::getId);
    finishedList.sort(cloudletComparator);

    try {
      java.io.File resultsDir = new java.io.File("migrations_results");
      if (!resultsDir.exists()) {
        resultsDir.mkdirs();
      }

      CsvTable csv = new CsvTable();
      csv.setPrintStream(new PrintStream(new java.io.File("migrations_results/migration_first_fit_policy.csv")));
      new CloudletsTableBuilder(finishedList, csv).build();

      CsvTable powerCsv = new CsvTable();
      powerCsv.setPrintStream(new PrintStream(new java.io.File("migrations_results/migration_first_fit_power.csv")));
      Shared.exportPowerConsumptionToCsv(hostList, simulation, powerCsv, "First Fit");

      // Print summary to console for verification
      new CloudletsTableBuilder(finishedList).build();
    } catch (IOException e) {
      System.err.println("Error writing CSV files: " + e.getMessage());
    }

    System.out.printf(
        "%nHosts CPU usage History (when the allocated MIPS is lower than the requested, it is due to VM migration overhead)%n");

    hostList.stream().filter(h -> h.getId() <= 2).forEach(Shared::printHostStateHistory);
    System.out.printf("Number of VM migrations: %d%n", migrationsNumber[0]);

    Shared.printPowerConsumptionSummary(hostList, simulation);

    System.out.println(getClass().getSimpleName() + " finished!");
  }

  private void startMigration(final VmHostEventInfo info) {
    Shared.startMigration(migrationsNumber, simulation, info);
  }

  private void finishMigration(final VmHostEventInfo info) {
    Shared.finishMigration(hostList, info);
  }

  public void createAndSubmitCloudlets(DatacenterBroker broker) {
    final List<Cloudlet> list = new ArrayList<>(Config.VM.PES.length);
    UtilizationModelDynamic um = Shared.createCpuUtilizationModel(Config.Cloudlet.INITIAL_CPU_PERCENTAGE, 1);

    for (Vm vm : vmList) {
      list.add(Shared.createCloudlet(vm, broker, um));
    }

    if (!list.isEmpty()) {
      list.get(list.size() - 1).setUtilizationModelCpu(
          Shared.createCpuUtilizationModel(Config.Cloudlet.INITIAL_CPU_PERCENTAGE, 1));
    }

    broker.submitCloudletList(list);
    System.out.println("Created " + list.size() + " cloudlets (including high-utilization ones to trigger migrations)");
  }

  public void createAndSubmitVms(DatacenterBroker broker) {
    final List<Vm> list = new ArrayList<>(Config.VM.PES.length);
    for (final int pes : Config.VM.PES) {
      list.add(Shared.createVm(pes));
    }

    vmList.addAll(list);
    broker.submitVmList(list);

    list.forEach(vm -> vm.addOnMigrationStartListener(this::startMigration));
    list.forEach(vm -> vm.addOnMigrationFinishListener(this::finishMigration));
    list.forEach(vm -> vm.enableUtilizationStats());
  }

  private Datacenter createDatacenter() {
    this.hostList = createHosts();
    System.out.println();

    this.allocationPolicy = new VmAllocationPolicyMigrationFirstFitStaticThreshold(
        new VmSelectionPolicyMinimumUtilization(),
        Config.Host.OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);
    this.allocationPolicy.setUnderUtilizationThreshold(Config.Host.UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);

    final Datacenter dc = new DatacenterSimple(simulation, hostList, allocationPolicy);
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
      list.add(Shared.createHost(pes, ram));
    }

    return list;
  }

  private void onVmsCreatedListener(final DatacenterBrokerEventInfo info) {
    System.out.printf("# All %d VMs submitted to the broker have been created.%n",
        broker.getVmCreatedList().size());

    // Ensure migration threshold is set and applied
    allocationPolicy.setOverUtilizationThreshold(Config.Host.OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);

    // Add a special listener that will check for possible VM migrations
    simulation.addOnClockTickListener(clock -> {
      if (clock.getTime() > 0 && clock.getTime() <= 2) {
        System.out.printf("%.2f: Checking hosts for potential VM migrations...%n", clock.getTime());

        // Manually check for overloaded hosts
        for (Host host : hostList) {
          double cpuUtilization = host.getCpuPercentUtilization();
          if (cpuUtilization > Config.Host.OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION) {
            System.out.printf("%.2f: Host %d is overloaded with %.1f%% CPU utilization (threshold: %.1f%%)%n",
                clock.getTime(), host.getId(), cpuUtilization * 100,
                Config.Host.OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION * 100);
          }
        }
      }
    });

    broker.removeOnVmsCreatedListener(info.getListener());
    vmList.forEach(vm -> Shared.showVmAllocatedMips(vm, vm.getHost(), info.getTime()));

    System.out.println();
    hostList.forEach(host -> Shared.showHostAllocatedMips(info.getTime(), host));
    System.out.println();
  }
}
