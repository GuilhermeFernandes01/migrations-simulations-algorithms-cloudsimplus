import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

plt.style.use('ggplot')
sns.set_palette("colorblind")

def load_data(migration_results_dir='../migrations_results'):
    """Load all CSV files with migration data from migrations_results directory"""
    data_files = {}
    results_path = Path(migration_results_dir)

    for file_path in results_path.glob('migration_*.csv'):
        strategy_name = file_path.stem.replace('migration_', '')
        if 'power' in strategy_name:
            df = pd.read_csv(file_path)
            data_files[strategy_name] = df
        elif 'policy' in strategy_name:
            df = pd.read_csv(file_path, skiprows=1)  # Skip header row for policy files
            data_files[strategy_name] = df

    return data_files

def analyze_power_consumption(power_data):
    """Analyze power consumption data from different strategies"""
    results = {}

    for strategy, df in power_data.items():
        if 'PowerConsumption(W)' in df.columns:
            total_row = df[df['Host'] == 'Total']

            if total_row.empty:
                active_hosts = df[df['CPUUtilization'] > 0].shape[0]
                total_power = df['PowerConsumption(W)'].sum()
                avg_power = df['PowerConsumption(W)'].mean()
                max_power = df['PowerConsumption(W)'].max()
                total_energy = df['TotalEnergyConsumption(Wh)'].sum()
            else:
                total_power = total_row['PowerConsumption(W)'].values[0]
                total_energy = total_row['TotalEnergyConsumption(Wh)'].values[0]

                df_without_total = df[df['Host'] != 'Total']
                active_hosts = df_without_total[df_without_total['CPUUtilization'] > 0].shape[0]
                avg_power = df_without_total['PowerConsumption(W)'].mean()
                max_power = df_without_total['PowerConsumption(W)'].max()

            results[strategy] = {
                'total_power_consumption': total_power,
                'average_power_consumption': avg_power,
                'max_power_consumption': max_power,
                'active_hosts': active_hosts,
                'total_energy_consumption': total_energy
            }

    return results

def plot_power_comparison(power_data):
    """Create comparative plots for power consumption across strategies"""
    power_dfs = {k: v for k, v in power_data.items() if 'power' in k}

    if not power_dfs:
        print("No power data found to plot")
        return

    os.makedirs('plots', exist_ok=True)

    plt.figure(figsize=(10, 6))
    results = analyze_power_consumption(power_dfs)

    strategies = [s.replace('_power', '') for s in results.keys()]
    total_energy = [results[s]['total_energy_consumption'] for s in results.keys()]

    plt.bar(strategies, total_energy)
    plt.title('Total Energy Consumption by Migration Strategy')
    plt.xlabel('Migration Strategy')
    plt.ylabel('Total Energy Consumption (Wh)')
    plt.tight_layout()
    plt.savefig('plots/total_energy_comparison.png')

    print(f"Plots saved to the 'plots' directory")

def analyze_migration_policies(policy_data):
    """Analyze migration policy data"""
    results = {}

    for strategy, df in policy_data.items():
        if 'policy' in strategy:
            total_vms = df.shape[0]
            avg_exec_time = df['ExecTime'].mean() if 'ExecTime' in df.columns else 0

            if 'Host' in df.columns and 'VM' in df.columns:
                vm_per_host = df.groupby('Host').size()
                max_vms_per_host = vm_per_host.max()
                min_vms_per_host = vm_per_host.min() if not vm_per_host.empty else 0
            else:
                max_vms_per_host = min_vms_per_host = 0

            results[strategy] = {
                'total_vms': total_vms,
                'average_execution_time': avg_exec_time,
                'max_vms_per_host': max_vms_per_host,
                'min_vms_per_host': min_vms_per_host
            }

    return results

def plot_vm_distribution(policy_data):
    """Plot VM distribution across hosts"""
    os.makedirs('plots', exist_ok=True)

    policy_dfs = {k: v for k, v in policy_data.items() if 'policy' in k}

    if not policy_dfs:
        print("No policy data found to plot")
        return

    for strategy, df in policy_dfs.items():
        strategy_name = strategy.replace('_policy', '')
        if 'Host' in df.columns:
            plt.figure(figsize=(12, 6))

            vm_counts = df.groupby('Host').size().sort_values(ascending=False)

            vm_counts.plot(kind='bar')
            plt.title(f'VM Distribution Across Hosts ({strategy_name})')
            plt.xlabel('Host ID')
            plt.ylabel('Number of VMs')
            plt.tight_layout()
            plt.savefig(f'plots/vm_distribution_{strategy_name}.png')

    print(f"VM distribution plots saved to the 'plots' directory")

def main():
    print("Starting migration simulation analysis...")

    data = load_data()

    power_data = {k: v for k, v in data.items() if 'power' in k}
    policy_data = {k: v for k, v in data.items() if 'policy' in k}

    os.makedirs('plots', exist_ok=True)

    if power_data:
        print("\nAnalyzing power consumption data...")
        power_results = analyze_power_consumption(power_data)

        for strategy, results in power_results.items():
            strategy_name = strategy.replace('_power', '')
            print(f"\n{strategy_name} power statistics:")
            for metric, value in results.items():
                print(f"  {metric}: {value:.2f}")

        plot_power_comparison(power_data)
    else:
        print("No power consumption data found")

    if policy_data:
        print("\nAnalyzing migration policy data...")
        policy_results = analyze_migration_policies(policy_data)

        for strategy, results in policy_results.items():
            strategy_name = strategy.replace('_policy', '')
            print(f"\n{strategy_name} policy statistics:")
            for metric, value in results.items():
                print(f"  {metric}: {value:.2f}")

        plot_vm_distribution(policy_data)
    else:
        print("No migration policy data found")

    if power_data:
        print("\nGenerating comparison table...")
        generate_comparison_table(power_results)

def generate_comparison_table(power_results):
    """Generate a comparison table for all strategies"""
    comparison_data = []

    for strategy, results in power_results.items():
        strategy_name = strategy.replace('_power', '')
        comparison_data.append({
            'Strategy': strategy_name,
            'Total Power (W)': results['total_power_consumption'],
            'Average Power (W)': results['average_power_consumption'],
            'Total Energy (Wh)': results['total_energy_consumption'],
            'Active Hosts': results['active_hosts']
        })

    comparison_df = pd.DataFrame(comparison_data)
    comparison_df.to_csv('plots/strategy_comparison.csv', index=False)

    print("\nStrategy Comparison:")
    print(comparison_df.to_string(index=False))

if __name__ == "__main__":
    main()
