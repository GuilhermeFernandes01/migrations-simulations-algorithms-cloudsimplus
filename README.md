# VM Migration Simulations with [CloudSim Plus](https://cloudsimplus.org/)

This repository contains the code for simulating VM migration algorithms and instructions to run the experiments and extract results.

## VM Migration Algorithms

The following VM migration algorithms are simulated in this project:
- **Best Fit**
- **Random**
- **Worst Fit**

## Running the Experiments

### Requirements
Ensure you have the following tools installed:
- [JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- [Maven 3.9.9](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/)

### Setup Instructions
1. Install [JDK 17+](https://www.oracle.com/java/technologies/downloads/), [Maven 3.9.9](https://maven.apache.org/download.cgi), and [Git](https://git-scm.com/).
2. Clone this repository:
  ```bash
  git clone https://github.com/GuilhermeFernandes01/migrations-simulations-algorithms-cloudsimplus
  ```
3. Navigate to the repository directory:
  ```bash
  cd migrations-simulations-algorithms-cloudsimplus
  ```

### Running Simulations
Run the VM migration simulations using the following command:
```bash
sh ./execute_migrations.sh
```

### Results
The simulation results will be saved in the `results` folder. Each migration algorithm will generate a separate CSV file:
- `migration_best_fit_policy.csv`
- `migration_random_policy.csv`
- `migration_worst_fit_policy.csv`

These files contain detailed data for each migration run.
