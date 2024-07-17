import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report
from sklearn.preprocessing import StandardScaler
import glob


def extract_features(df):
    features = []
    for column in df.columns:
        sensor_data = df[column]

        # Detect positive pulses
        pulses = sensor_data[sensor_data > sensor_data.mean()]

        # Extract features: height, duration, area, max, min, mean, std
        heights = pulses.values
        durations = np.diff(pulses.index)

        # Adjust lengths to match
        heights = heights[:-1]  # Drop the last height to match durations length

        areas = heights * durations
        max_height = np.max(heights) if len(heights) > 0 else 0
        min_height = np.min(heights) if len(heights) > 0 else 0
        mean_height = np.mean(heights) if len(heights) > 0 else 0
        std_height = np.std(heights) if len(heights) > 0 else 0
        pulse_frequency = len(heights) / len(sensor_data)

        features.append({
            'sensor': column,
            'heights': heights,
            'durations': durations,
            'areas': areas,
            'max_height': max_height,
            'min_height': min_height,
            'mean_height': mean_height,
            'std_height': std_height,
            'pulse_frequency': pulse_frequency
        })

    return features


def prepare_multiclass_dataset(features, state):
    X = []
    y = []
    for feature in features:
        heights = feature['heights']
        durations = feature['durations']
        areas = feature['areas']
        max_height = feature['max_height']
        min_height = feature['min_height']
        mean_height = feature['mean_height']
        std_height = feature['std_height']
        pulse_frequency = feature['pulse_frequency']

        for h, d, a in zip(heights, durations, areas):
            X.append([h, d, a, max_height, min_height, mean_height, std_height, pulse_frequency])
            y.append(state)

    return np.array(X), np.array(y)


def main():
    file_paths = glob.glob('data/*.csv')  # Adjust the path as necessary
    all_X = []
    all_y = []

    if not file_paths:
        print("No files found.")
        return

    for file_path in file_paths:
        # Extract state from the file name
        state = file_path.split('/')[-1].split('_')[0]
        print(f"Processing file: {file_path} with state: {state}")

        # Load and process the CSV file
        try:
            df = pd.read_csv(file_path)
        except Exception as e:
            print(f"Error loading {file_path}: {e}")
            continue

        df.columns = [f's{i}' for i in range(len(df.columns))]

        # Extract features
        features = extract_features(df)

        # Prepare dataset
        X, y = prepare_multiclass_dataset(features, state)

        all_X.append(X)
        all_y.append(y)

    if not all_X:
        print("No data to process.")
        return

    # Combine data from all files
    all_X = np.vstack(all_X)
    all_y = np.concatenate(all_y)

    print("Feature extraction and dataset preparation completed.")
    print(f"Total samples: {len(all_y)}")

    # Standardize the features
    scaler = StandardScaler()
    all_X = scaler.fit_transform(all_X)

    # Split dataset into training and testing sets
    X_train, X_test, y_train, y_test = train_test_split(all_X, all_y, test_size=0.2, random_state=42)

    # Define the parameter grid
    param_grid = {
        'n_estimators': [100, 200, 300],
        'max_features': ['sqrt', 'log2'],
        'max_depth': [10, 20, 30, None],
        'criterion': ['gini', 'entropy']
    }

    # Initialize the Random Forest classifier
    clf = RandomForestClassifier()

    # Initialize GridSearchCV
    grid_search = GridSearchCV(estimator=clf, param_grid=param_grid, cv=5, n_jobs=-1, verbose=2)

    # Fit the model
    grid_search.fit(X_train, y_train)

    # Predict and evaluate
    best_clf = grid_search.best_estimator_
    y_pred = best_clf.predict(X_test)
    print(classification_report(y_test, y_pred))
    print(f"Best Parameters: {grid_search.best_params_}")


# Run the main function
main()
