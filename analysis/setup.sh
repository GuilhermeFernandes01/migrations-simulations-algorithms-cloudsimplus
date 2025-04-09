#!/bin/bash

# Print colored messages
print_message() {
    echo -e "\e[1;34m$1\e[0m"
}

# Check for Python installation
if ! command -v python3 &> /dev/null; then
    echo "Python 3 is not installed. Please install Python 3 and try again."
    exit 1
fi

# Create directory for plots if it doesn't exist
mkdir -p plots

# Clean up any existing virtual environment that might be causing problems
if [ -d "venv" ]; then
    print_message "Removing existing virtual environment..."
    rm -rf venv
fi

# Create and activate virtual environment
print_message "Creating Python virtual environment..."
python3 -m venv venv

# Activate virtual environment
print_message "Activating virtual environment..."
source venv/bin/activate

# Install required packages
print_message "Installing required packages..."
pip install --upgrade pip
pip install --upgrade setuptools wheel
pip install --upgrade -r requirements.txt

# Create a simple README for the plots
if [ ! -f "plots/README.txt" ]; then
    echo "Migration Strategies Power Analysis Plots" > plots/README.txt
    echo "-----------------------------------------" >> plots/README.txt
    echo "" >> plots/README.txt
    echo "total_energy_comparison.png - Compares total energy consumption (Wh) across all strategies" >> plots/README.txt
    echo "total_power_comparison.png - Compares total power consumption (W) across all strategies" >> plots/README.txt
    echo "host_utilization.png - Shows the proportion of active vs inactive hosts for each strategy" >> plots/README.txt
    echo "power_distribution.png - Shows the distribution of power consumption values" >> plots/README.txt
    echo "cpu_vs_power.png - Shows the relationship between CPU utilization and power consumption" >> plots/README.txt
    echo "vm_distribution_*.png - Shows the VM distribution across hosts for each strategy" >> plots/README.txt
    echo "strategy_comparison.csv - CSV file with summary metrics for all strategies" >> plots/README.txt
fi

print_message "Setup complete! To run the analysis:"
print_message "1. Activate the virtual environment: source venv/bin/activate"
print_message "2. Run the analysis script: python analyze_migration.py"
