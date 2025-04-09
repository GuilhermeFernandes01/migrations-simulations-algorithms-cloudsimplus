#!/bin/bash

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Virtual environment not found. Running setup first..."
    ./setup.sh
else
    # Activate existing virtual environment
    source venv/bin/activate

    # Check if packages are installed
    if ! pip freeze | grep -q "pandas"; then
        echo "Required packages not found. Running setup again..."
        ./setup.sh
    fi
fi

# Activate virtual environment if not already activated
if [ -z "$VIRTUAL_ENV" ]; then
    source venv/bin/activate
fi

# Run the analysis script
echo "Running migration simulation analysis..."
python3 analyze_migration.py

# Show the results
echo -e "\nAnalysis complete! Results are available in the 'plots' directory."
echo "You can view the generated plots using your preferred image viewer."

# List generated plots
echo -e "\nGenerated plots:"
ls -1 plots/
