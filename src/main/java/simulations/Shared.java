package simulations;

public class Shared {
  public static final class Config {
    public static final class Scheduling {
      public static final int INTERVAL = 5;
    }

    public static final class Host {
      public static final double UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.1;
      public static final double OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.5;
      public static final int SEARCH_RETRY_DELAY = 60;
      public static final long BW = 16_000L; // Mb/s
      public static final int MIPS = 3000; // for each PE

      // Define different server types with their respective RAM and PEs
      // Small servers (64GB RAM, 16 cores) - 40 instances
      // Medium servers (128GB RAM, 32 cores) - 30 instances
      // Large servers (256GB RAM, 64 cores) - 20 instances
      // Very Large servers (512GB RAM, 128 cores) - 10 instances

      public static final long RAM[] = {
        // 40 small servers
        64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000,
        64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000,
        64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000,
        64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000, 64_000,

        // 30 medium servers
        128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000,
        128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000,
        128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000, 128_000,

        // 20 large servers
        256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000,
        256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000, 256_000,

        // 10 very large servers
        512_000, 512_000, 512_000, 512_000, 512_000, 512_000, 512_000, 512_000, 512_000, 512_000
      }; // host memory (MB)

      public static final int PES[] = {
        // 40 small servers
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16,

        // 30 medium servers
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32,

        // 20 large servers
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64,

        // 10 very large servers
        128, 128, 128, 128, 128, 128, 128, 128, 128, 128
      };

      public static final long STORAGE = 4_000_000; // 4TB storage per host
    }

    public static final class VM {
      // VM sizes based on real data center configurations (number of PEs/vCPUs)
      // Each array element creates one VM instance, so we need multiple entries
      // of each type to create enough VMs relative to the number of hosts
      public static final int PES[] = {
        // Create 30 small VMs (1 PE)
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,

        // Create 30 small VMs (2 PEs)
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,

        // Create 30 medium VMs (4 PEs)
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,

        // Create 25 large VMs (8 PEs)
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,

        // Create 20 xlarge VMs (16 PEs)
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,

        // Create 10 high-memory VMs (32 PEs)
        32, 32, 32, 32, 32, 32, 32, 32, 32, 32,

        // Create 5 very large VMs (64 PEs)
        64, 64, 64, 64, 64
      };

      public static final int MIPS = 2000; // for each PE
      public static final long SIZE = 1000; // image size (MB)
      public static final int RAM = 16_000; // VM memory (MB)
      public static final double BW = Config.Host.BW / (double) PES.length;
    }

    public static final class Cloudlet {
      public static final long LENGTH = 20_000;
      public static final long FILESIZE = 300;
      public static final long OUTPUTSIZE = 300;
      public static final double INITIAL_CPU_PERCENTAGE = 0.8;
      public static final double CPU_INCREMENT_PER_SECOND = 0.04;
    }

    public static final class Power {
      public static final double STATIC_POWER = 35;
      public static final int MAX_POWER = 50;
    }
  }
}
