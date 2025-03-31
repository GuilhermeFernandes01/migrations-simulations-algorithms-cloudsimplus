package simulations;

public class Shared {
  public static final class Config {
    public static final class Scheduling {
      public static final int INTERVAL = 1;
    }

    public static final class Host {
      public static final double UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.1;
      public static final double OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.7;
      public static final int SEARCH_RETRY_DELAY = 60;
      public static final long BW = 16_000L; // Mb/s
      public static final int MIPS = 1000; // for each PE
      public static final long RAM[] = { 15_000, 500_000, 25_000 }; // host memory (MB)
      public static final long STORAGE = 1_000_000; // host storage
      public static final int PES[] = { 4, 5, 5 };
    }

    public static final class VM {
      public static final int PES[] = { 2, 2, 2, 1 };
      public static final int MIPS = 1000; // for each PE
      public static final long SIZE = 1000; // image size (MB)
      public static final int RAM = 10_000; // VM memory (MB)
      public static final double BW = Config.Host.BW / (double) PES.length;
    }

    public static final class Cloudlet {
      public static final long LENGTH = 20_000;
      public static final long FILESIZE = 300;
      public static final long OUTPUTSIZE = 300;
      public static final double INITIAL_CPU_PERCENTAGE = 0.8;
      public static final double CPU_INCREMENT_PER_SECOND = 0.04;
    }
  }
}
