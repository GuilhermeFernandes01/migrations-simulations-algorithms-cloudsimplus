# Migration Simulation Analysis with [CloudSim Plus](https://cloudsimplus.org/)

This tool analyzes the results of VM migration simulations from CloudSim Plus, focusing on energy consumption.

## Features

- Analysis of energy consumption
- Visualization of energy consumption across different migration strategies
- Statistical summaries of simulation results

## Getting Started

### Prerequisites

- Python 3.6 or higher
- Result files in CSV format (migration_*_power.csv)

### Setup

Run the setup script to create a virtual environment and install the necessary dependencies:

```bash
chmod +x setup.sh
./setup.sh
```

### Running the Analysis

After running the setup script, you can execute the analysis:

```bash
source venv/bin/activate
python analyze_migration.py
```

Or simply use the execution script:

```bash
./run_analysis.sh
```

## Results

The analysis will produce:

1. Statistical summary in the console output
2. Visualization graphs in the `plots` directory:
  - Comparison of total energy consumption
3. CSV file with a comparative table of all strategies

## Energy Calculation Method

The energy calculation uses the following method:

1. For each host, the power (in Watts) is calculated based on CPU utilization using the PowerModelHostSimple model.
2. The total energy (in Watt-hours) is calculated by multiplying power by the simulation time (converted to hours).
3. The total energy consumption of the datacenter is the sum of the consumption of all hosts.

## Adding More Data

To analyze other simulation results:

1. Place your CSV result files in the `../migrations_results` directory.
2. The files must follow the naming convention: `migration_[strategy]_power.csv`.
3. Run the analysis script again.

## Understanding the Results

- **Energy Consumption**: Lower values indicate more energy-efficient strategies.
